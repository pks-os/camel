/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.converter.jaxp;

import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.apache.camel.TypeConverterLoaderException;
import org.apache.camel.spi.TypeConverterLoader;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.support.SimpleTypeConverter;
import org.apache.camel.support.TypeConverterSupport;
import org.apache.camel.util.DoubleMap;

/**
 * Generated by camel build tools - do NOT edit this file!
 */
@SuppressWarnings("unchecked")
public final class StaxConverterLoader implements TypeConverterLoader {

    public StaxConverterLoader() {
    }

    @Override
    public void load(TypeConverterRegistry registry) throws TypeConverterLoaderException {
        registerConverters(registry);
    }

    private void registerConverters(TypeConverterRegistry registry) {
        addTypeConverter(registry, java.io.InputStream.class, javax.xml.stream.XMLStreamReader.class, false,
            (type, exchange, value) -> getStaxConverter().createInputStream((javax.xml.stream.XMLStreamReader) value, exchange));
        addTypeConverter(registry, java.io.Reader.class, javax.xml.stream.XMLStreamReader.class, false,
            (type, exchange, value) -> getStaxConverter().createReader((javax.xml.stream.XMLStreamReader) value, exchange));
        addTypeConverter(registry, javax.xml.stream.XMLEventReader.class, java.io.File.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLEventReader((java.io.File) value, exchange));
        addTypeConverter(registry, javax.xml.stream.XMLEventReader.class, java.io.InputStream.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLEventReader((java.io.InputStream) value, exchange));
        addTypeConverter(registry, javax.xml.stream.XMLEventReader.class, java.io.Reader.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLEventReader((java.io.Reader) value));
        addTypeConverter(registry, javax.xml.stream.XMLEventReader.class, javax.xml.stream.XMLStreamReader.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLEventReader((javax.xml.stream.XMLStreamReader) value));
        addTypeConverter(registry, javax.xml.stream.XMLEventReader.class, javax.xml.transform.Source.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLEventReader((javax.xml.transform.Source) value));
        addTypeConverter(registry, javax.xml.stream.XMLEventWriter.class, java.io.OutputStream.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLEventWriter((java.io.OutputStream) value, exchange));
        addTypeConverter(registry, javax.xml.stream.XMLEventWriter.class, java.io.Writer.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLEventWriter((java.io.Writer) value));
        addTypeConverter(registry, javax.xml.stream.XMLEventWriter.class, javax.xml.transform.Result.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLEventWriter((javax.xml.transform.Result) value));
        addTypeConverter(registry, javax.xml.stream.XMLStreamReader.class, java.io.File.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLStreamReader((java.io.File) value, exchange));
        addTypeConverter(registry, javax.xml.stream.XMLStreamReader.class, java.io.InputStream.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLStreamReader((java.io.InputStream) value, exchange));
        addTypeConverter(registry, javax.xml.stream.XMLStreamReader.class, java.io.Reader.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLStreamReader((java.io.Reader) value));
        addTypeConverter(registry, javax.xml.stream.XMLStreamReader.class, java.lang.String.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLStreamReader((java.lang.String) value));
        addTypeConverter(registry, javax.xml.stream.XMLStreamReader.class, javax.xml.transform.Source.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLStreamReader((javax.xml.transform.Source) value));
        addTypeConverter(registry, javax.xml.stream.XMLStreamWriter.class, java.io.OutputStream.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLStreamWriter((java.io.OutputStream) value, exchange));
        addTypeConverter(registry, javax.xml.stream.XMLStreamWriter.class, java.io.Writer.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLStreamWriter((java.io.Writer) value));
        addTypeConverter(registry, javax.xml.stream.XMLStreamWriter.class, javax.xml.transform.Result.class, false,
            (type, exchange, value) -> getStaxConverter().createXMLStreamWriter((javax.xml.transform.Result) value));
    }

    private static void addTypeConverter(TypeConverterRegistry registry, Class<?> toType, Class<?> fromType, boolean allowNull, SimpleTypeConverter.ConversionMethod method) { 
        registry.addTypeConverter(toType, fromType, new SimpleTypeConverter(allowNull, method));
    }

    private volatile org.apache.camel.converter.jaxp.StaxConverter staxConverter;
    private org.apache.camel.converter.jaxp.StaxConverter getStaxConverter() {
        if (staxConverter == null) {
            staxConverter = new org.apache.camel.converter.jaxp.StaxConverter();
        }
        return staxConverter;
    }
}
