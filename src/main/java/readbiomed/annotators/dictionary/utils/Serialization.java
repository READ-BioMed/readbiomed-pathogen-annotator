package readbiomed.annotators.dictionary.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

import org.apache.uima.UimaContext;

public class Serialization {

	public static String serialize(Serializable o) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}

	public static Object deserialize(String s) throws IOException, ClassNotFoundException {
		byte[] data = Base64.getDecoder().decode(s);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
		Object o = ois.readObject();
		ois.close();
		return o;
	}
	
	public static Object readObject(UimaContext context, String objectParameter) throws ClassNotFoundException, IOException {
		String objectFileName = (String) context.getConfigParameterValue(objectParameter);
		try (ObjectInputStream ot = new ObjectInputStream(new GZIPInputStream(new FileInputStream(objectFileName)))) {
			return ot.readObject();
		}
	}
}