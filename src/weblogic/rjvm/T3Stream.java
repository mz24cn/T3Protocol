package weblogic.rjvm;

import java.io.IOException;

import util.BinTool;
import weblogic.common.internal.PeerInfo;
import weblogic.protocol.Protocol;
import weblogic.protocol.ProtocolManager;
import weblogic.protocol.ServerChannel;
import weblogic.rjvm.basic.BasicT3Connection;
import weblogic.server.channels.ServerChannelImpl;
import weblogic.utils.io.Chunk;


/**
 * <p>Title: T3Stream</p>
 * <p>Description: T3Stream is used to crack weblogic t3 data stream. 
 * The real reason why it was separated from BinTool is that weblogic.rjvm.RJVMImpl is
 * not visible outside the package. Please disable the signature of wlclient.jar and 
 * weblogic.jar otherwise this class will incur a SecurityException. </p>
 * <p>Running T3Stream requires both wlclient.jar and server-side weblogic.jar.<p>
 *
 * @author ZHANG Hua
 */

public class T3Stream {
	RJVMImpl rjvm;
	ServerChannel sc;
	Protocol pt3;
	ConnectionManager cm;
	MsgAbbrevJVMConnection majvmc;
	int BootstrapState;

	public T3Stream()
	{
		BootstrapState = -1;
		init();
	}

	public void init()
	{
    	JVMID id = JVMID.localID();
    	pt3 = ProtocolManager.getProtocolByName("t3");
        sc = new ServerChannelImpl().createImplicitChannel(pt3);

        majvmc = (MsgAbbrevJVMConnection)BinTool.createInstance(BasicT3Connection.class, sc, ServerChannel.class);
        majvmc.init(255, 19);
        
        rjvm = (RJVMImpl)BinTool.createInstance(RJVMImpl.class, id, JVMID.class, sc, ServerChannel.class, PeerInfo.VERSION_DIABLO, PeerInfo.class);
        cm = new ConnectionManagerClient(rjvm);
	}
	
	public OutputStream getOutputStream() throws IOException
	{
		return new OutputStream(rjvm.getRequestStream(sc, pt3));
	}
	
	public MsgAbbrevInputStream getInputStream(Chunk head) throws ClassNotFoundException, IOException
	{
		//repeat t3 protocol bootstrap: ignore 
		if ( ((head.buf[7]|head.buf[8]|head.buf[9])==0 && (head.buf[10]&0xFF)<BootstrapState && head.buf[10]!=0) //repeatly sending:2->5;对于buf[7-10]==0的情况，可能是心跳检测
			|| ((head.buf[7]&head.buf[8]&head.buf[9]&head.buf[10])==-1 && BootstrapState!=-1) ) //repeatly sending:1
			return null;
		else if (BootstrapState < 3)
			BootstrapState = head.buf[10]==-1? 0 : head.buf[10];

		MsgAbbrevInputStream mais = new MsgAbbrevInputStream(cm);
		mais.init(head, majvmc);
		return mais;
	}
	
	public int getBootstrapState()
	{
		return BootstrapState;
	}

	public class OutputStream {
		MsgAbbrevOutputStream maos;
		OutputStream(MsgAbbrevOutputStream maos)
		{
			this.maos = maos;
		}
		public void write(int i)
		{
			maos.writeLength(i);
		}
		public void writeObject(Object o) throws IOException
		{
			maos.writeObjectWL(o);
		}
		public void writeImmutable(Object o) throws IOException
		{
			maos.writeImmutable(o);
		}
		public void reset()
		{
			maos.reset();
		}
		public void writeTo(java.io.OutputStream os) throws IOException
		{
			maos.writeTo(os);
		}
	}
}
