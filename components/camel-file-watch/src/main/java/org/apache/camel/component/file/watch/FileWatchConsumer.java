/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.file.watch;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.file.watch.constants.FileEvent;
import org.apache.camel.component.file.watch.utils.PathUtils;
import org.apache.camel.support.DefaultConsumer;
import org.apache.camel.util.AntPathMatcher;

/**
 * The file-watch consumer.
 */
public class FileWatchConsumer extends DefaultConsumer {

    private ExecutorService watchDirExecutorService;
    private ExecutorService pollExecutorService;
    private LinkedBlockingQueue<FileEvent> eventQueue;
    private Path baseDirectory;
    private AntPathMatcher antPathMatcher;
    private DirectoryWatcher watcher;

    public FileWatchConsumer(FileWatchEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        if (getEndpoint().getQueueSize() <= 0) {
            eventQueue = new LinkedBlockingQueue<>();
        } else {
            eventQueue = new LinkedBlockingQueue<>(getEndpoint().getQueueSize());
        }

        antPathMatcher = new AntPathMatcher();
        baseDirectory = Paths.get(getEndpoint().getPath());
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        if (!Files.exists(baseDirectory)) {
            if (getEndpoint().isAutoCreate()) {
                baseDirectory = Files.createDirectories(baseDirectory);
            } else {
                throw new RuntimeCamelException("Path must exists when autoCreate = false");
            }
        }

        if (!Files.isDirectory(baseDirectory)) {
            throw new RuntimeCamelException(String.format("Parameter path must be directory, %s given", baseDirectory.toString()));
        }

        DirectoryWatcher.Builder watcherBuilder = DirectoryWatcher.builder()
        .path(this.baseDirectory)
        .logger(log)
        .listener(new FileWatchDirectoryChangeListener());

        if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
            // If not macOS, use FileSystem WatchService. io.methvin.watcher uses by default WatchService associated to default FileSystem.
            // We need per FileSystem WatchService, to allow monitoring on machine with multiple file systems.
            // Keep default for macOS
            watcherBuilder.watchService(this.baseDirectory.getFileSystem().newWatchService());
        }

        watcherBuilder.fileHashing(getEndpoint().isUseFileHashing());
        if (getEndpoint().getFileHasher() != null && getEndpoint().isUseFileHashing()) {
            watcherBuilder.fileHasher(getEndpoint().getFileHasher());
        }

        this.watcher = watcherBuilder.build();

        watchDirExecutorService = getEndpoint().getCamelContext().getExecutorServiceManager()
        .newFixedThreadPool(this, "CamelFileWatchService", getEndpoint().getPollThreads());
        pollExecutorService = getEndpoint().getCamelContext().getExecutorServiceManager()
        .newFixedThreadPool(this, "CamelFileWatchPoll", getEndpoint().getConcurrentConsumers());

        for (int i = 0; i < getEndpoint().getPollThreads(); i++) {
            this.watcher.watchAsync(watchDirExecutorService);
        }
        for (int i = 0; i < getEndpoint().getConcurrentConsumers(); i++) {
            pollExecutorService.submit(new PollRunnable());
        }
    }

    @Override
    protected void doStop() throws Exception {
        this.watcher.close();
        getEndpoint().getCamelContext().getExecutorServiceManager().shutdownNow(watchDirExecutorService);
        getEndpoint().getCamelContext().getExecutorServiceManager().shutdownNow(pollExecutorService);
        super.doStop();
    }

    @Override
    protected void doSuspend() throws Exception {
        doStop();
    }

    @Override
    protected void doResume() throws Exception {
        doStart();
    }

    private Exchange prepareExchange(FileEvent event) {
        Exchange exchange = getEndpoint().createExchange();
        exchange.setFromEndpoint(getEndpoint());
        File file = event.getEventPath().toFile();
        Message message = exchange.getIn();
        message.setBody(file);
        message.setHeader(FileWatchComponent.EVENT_TYPE_HEADER, event.getEventType());
        message.setHeader(Exchange.FILE_NAME, PathUtils.normalizeToString(baseDirectory.relativize(event.getEventPath())));
        message.setHeader("CamelFileRelativePath", message.getHeader(Exchange.FILE_NAME));
        message.setHeader("CamelFileAbsolutePath", PathUtils.normalizeToString(event.getEventPath().toAbsolutePath()));
        message.setHeader("CamelFileAbsolute", true);
        message.setHeader(Exchange.FILE_PARENT, PathUtils.normalizeToString(event.getEventPath().getParent().toAbsolutePath()));
        message.setHeader(Exchange.FILE_NAME_ONLY, event.getEventPath().getFileName().toString());

        return exchange;
    }

    private boolean matchFilters(FileEvent fileEvent) {
        if (!getEndpoint().getEvents().contains(fileEvent.getEventType())) {
            return false;
        }

        if (!getEndpoint().isRecursive() && !Objects.equals(fileEvent.getEventPath().getParent(), this.baseDirectory)) {
            // On some platforms (macOS) is WatchService always recursive,
            // so we need to filter this out to make this component platform independent
            return false;
        }

        String pattern = getEndpoint().getAntInclude();
        if (pattern == null || pattern.trim().isEmpty()) {
            return true;
        }

        return antPathMatcher.match(
            getEndpoint().getAntInclude(),
            PathUtils.normalizeToString(baseDirectory.relativize(fileEvent.getEventPath())) // match against relativized path
        );
    }

    @Override
    public FileWatchEndpoint getEndpoint() {
        return (FileWatchEndpoint) super.getEndpoint();
    }

    class FileWatchDirectoryChangeListener implements DirectoryChangeListener {
        @Override
        public void onEvent(DirectoryChangeEvent directoryChangeEvent) {
            if (directoryChangeEvent.eventType() == DirectoryChangeEvent.EventType.OVERFLOW) {
                log.warn("OVERFLOW occurred, some events may be lost. Consider increasing of option 'pollThreads'");
                return;
            }
            FileEvent fileEvent = new FileEvent(directoryChangeEvent);
            if (matchFilters(fileEvent)) {
                eventQueue.offer(fileEvent);
            }
        }

        @Override
        public boolean isWatching() {
            return !isStoppingOrStopped() && !isSuspendingOrSuspended();
        }

        @Override
        public void onException(Exception e) {
            handleException(e);
        }
    }

    class PollRunnable implements Runnable {
        @Override
        public void run() {
            while (!isStoppingOrStopped() && !isSuspendingOrSuspended()) {
                FileEvent event;
                try {
                    event = eventQueue.poll(1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    return;
                }

                if (event != null) {
                    try {
                        Exchange exchange = prepareExchange(event);
                        getProcessor().process(exchange);
                    } catch (Throwable t) {
                        handleException(t);
                    }
                }
            }
        }
    }
}
