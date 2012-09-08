package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import org.jboss.aop.joinpoint.MethodInvocation;
import org.jboss.remoting.InvocationRequest;

import test.DebugState_Init;
import weblogic.rjvm.MsgAbbrevInputStream;
import weblogic.rjvm.T3Stream;
import weblogic.rmi.cluster.Version;
import weblogic.utils.io.Chunk;

//import com.ibm.rational.test.lt.kernel.IDataArea;
//import com.ibm.rational.test.lt.kernel.services.ITestExecutionServices;
//import com.ibm.rational.test.lt.kernel.services.ITestInfo;
//import com.ibm.rational.test.lt.kernel.services.IVirtualUserInfo;
//import com.ipacs.framework.event.EventSupport;

/**
 * <p>Title: BinTool</p>
 * <p>Description: BinTool</p>
 *
 * @author ZHANG Hua
 */

@SuppressWarnings("unchecked")
public class BinTool<Event> {
	
	private static final long serialVersionUID = -6567145497967416098L;
//  The method below incurs java.lang.NoClassDefFoundError: initialization failure
//	private static ALSLogger logger = ALSLogger.getLogger(BinTool.class);
	
	private static byte[] HEAD = {(byte) 0x0AC, (byte) 0x0ED, 0x00, 0x05};
	private static T3Stream weblogic = null;
	public static Random rand = new Random(System.currentTimeMillis());
	public static boolean DEBUG = false;
	
	public static int checkStreamHeader(final byte[] data)
	{
		if (data.length>12 && data[0]=='s' && data[1]=='r' && data[4]=='o' && data[5]=='r' && data[6]=='g' && data[7]=='.' && data[8]=='j' && data[9]=='b' && data[10]=='o' && data[11]=='s' && data[12]=='s')
			return 1;
		else if (data.length>19 && data.length==((data[0]&0xFF)<<24)+((data[1]&0xFF)<<16)+((data[2]&0xFF)<<8)+(data[3]&0xFF))
			return 2;
		else
			return 0;
	}

	public static <Event> Map getDataMap(byte[] data, Object[] attachments) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException
	{
		int AppServer = checkStreamHeader(data);
		if (AppServer <= 0)
			return null;
			
		Object[] attach = new Object[1];
		Event event = null;
		if (AppServer == 1) //jboss
			event = getEventJboss(data, attach);
		else //weblogic
			event = getEventWeblogic(data, attach);
		if (event == null) {
			attachments[0] = (attach[0] instanceof byte[])? (byte[])attach[0] : data;
			return null;
		}
		
//		Object oldmap = getField(event, EventSupport.class, "mapViewData");
//		Map map = event.loadData();

		attachments[0] = AppServer;
		attachments[1] = event;
//		attachments[2] = oldmap;
		attachments[3] = attach[0];
		attachments[4] = data;
//		return map;
		return null;
	}
	
	public static <Event> byte[] setDataMap(Map map, Object[] attachments) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, IOException
	{
		int AppServer = new Integer(attachments[0].toString());
		Event event = (Event)attachments[1];
		Object oldmap = attachments[2];
		Object attachment = attachments[3];
		byte[] olddata = (byte[])attachments[4];

//		event.setViewData(map);
//		setField(event, EventSupport.class, "mapViewData", oldmap);

		if (AppServer == 1) //jboss
			return assembleEventJboss((InvocationRequest)attachment);
		else //weblogic
			return assembleEventWeblogic(event, (Version)attachment, olddata);
	}
	
	public static <Event> byte[] modifyStream(byte[] data, Object... args) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, IOException
	{
		Object attachments[] = new Object[5];
		Map map = getDataMap(data, attachments);
		if (map==null || map.size()==0 || args.length==0)
			return (attachments[0] instanceof byte[])? (byte[])attachments[0] : data;
//test.Class_SEBooking_Insert.map = map;
		long t = System.currentTimeMillis();
//		if (DEBUG) {
//			Event event = (Event)attachments[1];
//			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("C:\\MAP_DATA\\map_"+event.getEventName()+".serial"));
//			oos.writeObject(map);
//			oos.close();
//		}
		if (args.length%2==1 && new Boolean(args[args.length-1].toString())==false)
			for (int i=0; i<args.length-1; i+=2)
				traverseMap(map, args[i], args[i+1]);
		else
			for (int i=0; i<args.length; i+=2)
				traverseMapV(map, args[i], args[i+1]);
		if (false && DEBUG) {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("C:\\map"+t+".new.serial"));
			oos.writeObject(map);
			oos.close();
		}
		
		return setDataMap(map, attachments);
	}
	
	public static void traverseMap(Map map, Object K, Object V)
	{
		if (map.containsKey(K))
			map.put(K, map.get(K) instanceof String? V.toString() : V);
		for (Object key : map.keySet()) {
			Object value = map.get(key);
			if (value instanceof Map)
				traverseMap((Map)value, K, V);
			else if (value instanceof List)
				traverseList((List)value, K, V);
		}
	}
	
	public static void traverseList(List list, Object K, Object V)
	{
		for (Object value : list)
			if (value instanceof Map)
				traverseMap((Map)value, K, V);
			else if (value instanceof List)
				traverseList((List)value, K, V);
	}
	
	public static void traverseMapV(Map map, Object old, Object V)
	{
		for (Object key : map.keySet()) {
			Object value = map.get(key);
			if (old.equals(value))
				map.put(key, map.get(key) instanceof String? V.toString() : V);
			else if (value instanceof Map)
				traverseMap((Map)value, old, V);
			else if (value instanceof List)
				traverseList((List)value, old, V);
		}
	}
	
	public static void traverseListV(List list, Object old, Object V)
	{
		for (int i=list.size()-1; i>=0; i--) {
			Object value = list.get(i);
			if (old.equals(value))
				list.add(i, list.get(i) instanceof String? V.toString() : V);
			else if (value instanceof Map)
				traverseMap((Map)value, old, V);
			else if (value instanceof List)
				traverseList((List)value, old, V);
		}
	}
	
	public static <Event> Event getEventJboss(final byte[] data, Object[] attach)
	{
		Event event = null;
		try {
			byte[] headAddon = new byte[data.length+HEAD.length]; // headAddon: InvocationRequest serialized.
			System.arraycopy(HEAD, 0, headAddon, 0, HEAD.length);
			System.arraycopy(data, 0, headAddon, HEAD.length, data.length);
			
			ByteArrayInputStream bais = new ByteArrayInputStream(headAddon);
			ObjectInputStream ois = new ObjectInputStream(bais);
			InvocationRequest IR = (InvocationRequest)ois.readObject();
			ois.close();
			bais.close();//All bytes in data are read.
			
			MethodInvocation MI = (MethodInvocation)IR.getParameter();
			Object[] array = MI.getArguments();
			event = (Event)array[0];
			if (attach != null)
				attach[0] = IR;
		}
		catch (Exception e) {
			record("C:\\Exception.txt", e);
//			logger.logError(e);
		}
		catch (Error e) {
			record("C:\\Error.txt", e);
//			logger.logError(e);
		}
		return event;
	}
	
	public static <Event> Event getEventWeblogic(final byte[] data, Object[] attach)
	{
		Event event = null;
		Version ver = null;
		try {
			if (weblogic == null) {
				System.setProperty("javax.xml.parsers.SAXParserFactory", "com.ibm.xml.xlxp.api.jaxp.impl.SAXParserFactoryImpl");
				weblogic = new T3Stream();
			}
			
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			Chunk head = Chunk.getChunk();			
			Chunk.chunkFully(head, bais);
			bais.close();
			synchronized (weblogic) {
				MsgAbbrevInputStream mais = weblogic.getInputStream(head);
				
				//multiuser condition: modify JVMID in repeat bootstrap  
				if ((head.buf[7]&head.buf[8]&head.buf[9]&head.buf[10])==-1 || //协议引导：发送JVMID
					(head.buf[7]|head.buf[8]|head.buf[9]|head.buf[10])==0) { //可能是连接心跳检测：发送JVMID
//					int heartBeat = mais.readInt();
//					int keySize = mais.readInt();
//					byte[] JVMPublicKey = new byte[keySize];
//					mais.read(JVMPublicKey);
//					int ImmutableNum = mais.read();
//					weblogic.common.internal.PeerInfo PI = (weblogic.common.internal.PeerInfo)mais.readObjectWL();

					byte[] newdata = new byte[data.length];
					System.arraycopy(data, 0, newdata, 0, data.length);
					byte[] JVMID = new byte[8];
					rand.nextBytes(JVMID);
					byte[] JvmIdName = "weblogic.rjvm.JVMID".getBytes();
					int n = searchBytes(newdata, JvmIdName, 0) + 19;
					n = searchBytes(newdata, JvmIdName, n) + 35;
					System.arraycopy(JVMID, 0, newdata, n, JVMID.length);
					attach[0] = newdata;
					return null;
				}
				if (mais == null)
					return null;

				Object o = mais.readObjectWL();
//				if (o instanceof Event)
				event = (Event)o;
				o = mais.readObjectWL();
				if (o instanceof Version)
					ver = (Version)o;
				if (attach != null)
					attach[0] = ver;
			}
		}
		catch (Exception e) {
			record("C:\\Exception.txt", e);
//			logger.logError(e);
		}
		catch (Error e) {
			record("C:\\Error.txt", e);
//			logger.logError(e);
		}
		return event;
	}
	
	public static byte[] assembleEventJboss(InvocationRequest IR) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(IR);
		oos.close();
		baos.close();
		
		byte[] headAddon = baos.toByteArray();
		byte[] data = new byte[headAddon.length-HEAD.length];
		System.arraycopy(headAddon, HEAD.length, data, 0, headAddon.length-HEAD.length);
		return data;
	}
	
	public static <Event> byte[] assembleEventWeblogic(Event event, Version ver, byte[] origin) throws IOException
	{
		T3Stream.OutputStream os = weblogic.getOutputStream();
		os.write(1);//total number of ServiceContext
		os.write(1);//ServiceContext index
		os.writeImmutable(null);//boolean, and make immutableNum to be 1
		os.writeObject(event);
		os.writeObject(new weblogic.rmi.cluster.Version(ver.getVersion()+System.currentTimeMillis()%1000000));
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
		os.writeTo(baos);
		int newOffset = baos.size();
		
		int tableOffset = ((origin[15]&0xFF)<<24)+((origin[16]&0xFF)<<16)+((origin[17]&0xFF)<<8)+(origin[18]&0xFF);
		baos.write(origin, tableOffset, origin.length-tableOffset);
		int totalBytes = baos.size();
		byte[] data = baos.toByteArray();
		System.arraycopy(origin, 0, data, 0, 22);
		
		data[0] = (byte)(totalBytes>>24);
		data[1] = (byte)((totalBytes&0x0FF0000)>>16);
		data[2] = (byte)((totalBytes&0xFF00)>>8);
		data[3] = (byte)(totalBytes&0x0FF);
		
		data[15] = (byte)(newOffset>>24);
		data[16] = (byte)((newOffset&0x0FF0000)>>16);
		data[17] = (byte)((newOffset&0xFF00)>>8);
		data[18] = (byte)(newOffset&0x0FF);
		
		os.reset();
		return data;
	}
	
//	public static void logTime(ITestExecutionServices testExecutionServices, boolean isSend)
//	{
//		try {
//			IDataArea DA = testExecutionServices.findDataArea(IDataArea.VIRTUALUSER);
//			IVirtualUserInfo userInfo = (IVirtualUserInfo)DA.get("VirtualUserInfo");
//			int userId = userInfo.getUID();
//			DA = testExecutionServices.findDataArea(IDataArea.TEST);
//			ITestInfo testInfo = (ITestInfo)DA.get("TestInfo");
//			String testName = testInfo.getName();
//			
//			FileWriter fw = new FileWriter(DebugState_Init.timeLogFileDir+testName+"_User"+userId+".log", true);
//			long timestamp = testExecutionServices.getTime().timeInTest();
////			long timestamp = System.currentTimeMillis();
//			fw.write(isSend? timestamp+"("+Thread.currentThread().getId()+' '+isSend+")," : timestamp+"("+Thread.currentThread().getId()+' '+isSend+")\r\n");
//			fw.close();
//		} catch (Exception e) {
//			BinTool.record("C:\\Exception.txt", e);
//		}
//	}
	
	public static Object getField(Object O, Class C, String name) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException 
	{
		Field Attr = (C==null? O.getClass() : C).getDeclaredField(name);
		Attr.setAccessible(true);
		return Attr.get(O);
	}
	
	public static boolean setField(Object O, Class C, String name, Object V) 
	{
		try {
			Field Attr = (C==null? O.getClass() : C).getDeclaredField(name);
			Attr.setAccessible(true);
			Attr.set(O, V);
		} catch (SecurityException e) {
			return false;
		} catch (NoSuchFieldException e) {
			return false;
		} catch (IllegalArgumentException e) {
			return false;
		} catch (IllegalAccessException e) {
			return false;
		}
		return true;
	}
	
	public static Object invokeMethod(Object O, Class C, String name, Object... args)
	{
		Method M;
		Object R = null;
		int n = args.length>>1;
		Class[] classes = new Class[n];
		Object[] objects = new Object[n];
		for (int i=0,j=0; i<n; i++,j+=2) {
			classes[i] = (Class)args[j+1];
			objects[i] = args[j];
		}
		try {
			M = C.getDeclaredMethod(name, classes);
			M.setAccessible(true);
			R = M.invoke(O, objects);
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
		return R;
	}
	
	public static Object createInstance(Class C, Object... args)
	{
		Constructor INIT;
		Object O = null;
		int n = args.length>>1;
		Class[] classes = new Class[n];
		Object[] objects = new Object[n];
		for (int i=0,j=0; i<n; i++,j+=2) {
			classes[i] = (Class)args[j+1];
			objects[i] = args[j];
		}
		try {
			INIT = C.getDeclaredConstructor(classes);
			INIT.setAccessible(true);
			O = INIT.newInstance(objects);
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
		return O;
	}
	
	//byte[] search
	public static int searchBytes(byte[] target, byte[] keyword, int start)
	{
		int iEnd = target.length - keyword.length;
		for (int i=start; i<iEnd; i++) {
			int kEnd = i + keyword.length, n=0;
			for (int k=i; k<kEnd; k++, n++)
				if (target[k] != keyword[n])
					break;
			if (n >= keyword.length)
				return i;
		}
		return -1;
	}

	public static Vector<Object> findObject(final byte[] data, boolean addHead)
	{
		byte[] HEAD = {(byte) 0x0AC, (byte) 0x0ED, 0x00, 0x05};
		byte[] objBytes = new byte[data.length+HEAD.length];
		Vector<Object> V = new Vector<Object>();
		for (int i=0; i<data.length; i++) {
			if (addHead) {
				System.arraycopy(HEAD, 0, objBytes, 0, HEAD.length);
				System.arraycopy(data, i, objBytes, HEAD.length, data.length-i);
			}
			else
				System.arraycopy(data, i, objBytes, 0, data.length-i);
			ByteArrayInputStream bais = new ByteArrayInputStream(objBytes);
			ObjectInputStream ois;
			try {
				ois = new ObjectInputStream(bais);
				Object O = ois.readObject();
				V.add(O);
				ois.close();
				bais.close();//All bytes in data are read.
//				System.out.println("found@"+i);
			} catch (Exception e) {
			} catch (Error e) {
//				System.out.println("Error:"+e.getMessage());
			}
		}
		return V;
	}
	
	//record data to file for test
	public static void record(String filename, byte[] data)
	{
		record(filename, data, null, false);
	}
	public static void record(String filename, Throwable t)
	{
		record(filename, null, t, true);
	}
	public static void record(String filename, byte[] data, Throwable t, boolean append)
	{
		try {
			FileOutputStream fos = new FileOutputStream(filename, append);
			if (data != null) {
				fos.write(data);
				if (data.length>1 && append) {
					fos.write(0x0D);
					fos.write(0x0A);
				}
			}
			if (t != null) {
				t.printStackTrace(new PrintStream(fos));
				fos.write(0x0D);
				fos.write(0x0A);
			}
			if (append && data!=null && data.length>1) {
				fos.write(0x0D);
				fos.write(0x0A);
				fos.write(0x0D);
				fos.write(0x0A);
			}
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void reviseDecompiledSource(File srcDir)
	{
//		File dir = new File(srcDir);
		File[] files = srcDir.listFiles();
		for (File theFile : files) {
			if (theFile.isDirectory())
				reviseDecompiledSource(theFile);
			else if (theFile.getName().endsWith(".java")) {
				try {
					LinkedList<String> lines = new LinkedList<String>();
					FileReader fr = new FileReader(theFile);
					LineNumberReader lnr = new LineNumberReader(fr);
					
					String line = null;
					while ((line=lnr.readLine()) != null) {
						lines.add(line);
						if (line.startsWith("/*")) {
							int end = line.indexOf("*/")-1;
							if (end < 3)
								throw new StringIndexOutOfBoundsException(theFile.getAbsolutePath());
							String num = line.substring(3, end).trim();
							if (num.length() > 0) {
								int sourceLine = new Integer(num).intValue();
								int lastLine = lines.size();
								if (lastLine < sourceLine)
									for (int i=sourceLine; i>lastLine; i--)
										lines.add(lastLine-1, "");
								else if (lastLine > sourceLine) {
									StringBuffer sb = new StringBuffer(512);
									for (int i=sourceLine; i<lastLine; i++) {
										String tmp = lines.get(sourceLine-2);
//										int end2 = line.indexOf("*/")-1;
//										String num2 = tmp.substring(3, end2).trim();
//										if (num2.length() > 0) {
//											int belowLine = new Integer(num2).intValue();
//											if (belowLine > lastLine)
//												for (int j=belowLine; j>lastLine; j--)
//													lines.add("");
//											lines.set(belowLine-1, sb.toString());
//										}
										sb.append(tmp).append(' ');
										lines.remove(sourceLine-2);
									}
									lines.set(sourceLine-2, sb.toString());
								}
							}
						}
						else
							break;
					}
					lnr.close();
					fr.close();

					FileWriter fw = new FileWriter(theFile);
					for (String newline : lines) {
						fw.write(newline);
						fw.write("\r\n");
//						System.out.println(newline);
					}
					fw.close();
				} catch (FileNotFoundException e) {
				} catch (IOException e) {
				} catch (StringIndexOutOfBoundsException e) {
					System.out.println(e);
				}
			}
		}
	}
//	private boolean insertLine(String line, LinkedList<String> lines)
//	{
//		if (line.startsWith("/*")) {
//			int end = line.indexOf("*/")-1;
//			if (end < 3)
//				return true;
//			String num = line.substring(3, end).trim();
//			if (num.length() > 0) {
//				int sourceLine = new Integer(num).intValue();
//				int lastLine = lines.size();
//				if (lastLine < sourceLine)
//					for (int i=sourceLine; i>lastLine; i--)
//						lines.add(lastLine-1, "");
//				else if (lastLine > sourceLine) {
//					StringBuffer sb = new StringBuffer(512);
//					for (int i=sourceLine; i<lastLine; i++) {
//						sb.append(lines.get(sourceLine-2)).append(' ');
//						lines.remove(sourceLine-2);
//					}
//					lines.set(sourceLine-2, sb.toString());
//				}
//			}
//		}
//		else
//			return false;
//	}

	public static void main(String argv[])
	{
		reviseDecompiledSource(new File("E:\\KFF\\wlclient.lineok"));
	}
	
	public static Map getEventMap(String filename)
	{
		Map map = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream("C:\\MAP_DATA\\"+filename));
			map = (Map)ois.readObject();
			ois.close();
		} catch (IOException e) {
			record("C:\\Exception.txt", e);
		} catch (ClassNotFoundException e) {
			record("C:\\Exception.txt", e);
		}
		return map;
	}
	
	public static String getServerByRandom()
	{
		final String servers[] = {"t3://172.21.140.76:7001/","t3://172.21.140.76:7002/","t3://172.21.140.78:7003/","t3://172.21.140.78:7004/"};
		return servers[rand.nextInt(4)];
	}
}
