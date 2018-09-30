package bpiwowar.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.log4j.Logger;

/**
 * @author bpiwowar
 */
public class OptionSerializer {
    static final Logger logger = Logger.getLogger(OptionSerializer.class);

    public static final void setOptions(OutputStream os, Object options) {
        try {
            JAXBContext context = JAXBContext.newInstance(options.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(options, os);
        }
        catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static final void setOptions(File file, Object options) throws FileNotFoundException {
        setOptions(new FileOutputStream(file), options);
    }

    public static final <T> T getOptions(InputStream is,
        Class<T> marshalledClass) {
        try {
            JAXBContext context = JAXBContext.newInstance(marshalledClass);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            unmarshaller.setProperty("jaxb.formatted.output", true);
            unmarshaller.setProperty("jaxb.fragment", true);
            unmarshaller.setSchema(null); // No Schema
            Object options = unmarshaller.unmarshal(is);

            // We check before if the class is alright
            if (!options.getClass().equals(marshalledClass))
                throw new ClassCastException("Cannot cast "
                    + options.getClass() + " to " + marshalledClass);

            @SuppressWarnings("unchecked") final T castedOptions = (T)options;

            return castedOptions;
        }
        catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getOptions(File file, Class<T> marshalledClass)
        throws FileNotFoundException {
        return getOptions(new FileInputStream(file), marshalledClass);
    }

}
