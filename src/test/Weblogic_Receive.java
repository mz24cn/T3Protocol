package test;

import java.io.FileWriter;

import util.BinTool;

import com.ibm.rational.test.lt.kernel.IDataArea;
import com.ibm.rational.test.lt.kernel.services.ITestExecutionServices;
import com.ibm.rational.test.lt.kernel.services.ITestInfo;
import com.ibm.rational.test.lt.kernel.services.IVirtualUserInfo;
import com.ibm.rational.test.lt.execution.socket.custom.ISckCustomReceivePolicy;
import com.ibm.rational.test.lt.execution.socket.custom.ISckReceiveAction;

/**
 *  Custom receive policy <b>Weblogic_Receive</b>.<br>
 *  For javadoc of ITestExecutionServices, select 'Help Contents' in the Help menu and select
 *  'Extending Rational Performance Tester functionality' -> 'Extending test execution with custom code'
 */
public class Weblogic_Receive implements ISckCustomReceivePolicy {

	// static {
	//	 static blocks are called once on each run and allow for example to bind
	//	 to an external dynamic library
	// }

	ISckReceiveAction receiveAction;
	ITestExecutionServices testExecutionServices;

	public Weblogic_Receive() {
		// The constructor is called during the test creation, not at the time of the execution of
		// the customized receive action
	}

	public void setup(ITestExecutionServices tesRef,
			ISckReceiveAction receiveActionRef) {
		testExecutionServices = tesRef;
		receiveAction = receiveActionRef;
		/* Do here any custom setup */

	}

	public boolean onRead(int readByte) {
		/* Make here your custom decision */

		try {
			byte[] bytes = null;
			
			IDataArea DA = testExecutionServices.findDataArea(IDataArea.VIRTUALUSER);
			Integer Size = (Integer)DA.get("ResponseSize");
			if (Size != null) {
				Integer Num = (Integer)DA.get("ReadByteNum") + 1;
				if (Num < Size)
					DA.put("ReadByteNum", Num);
				else {
BinTool.record("C:\\Receive.txt", ("End:"+Num+" "+readByte).getBytes(), null, true);
					DA.remove("ResponseSize");
					DA.remove("ReadByteNum");
					DA.remove("SizeBytes");
					receiveAction.receiveSuccess();
					return true;
				}
			}
			else {
				bytes = (byte[])DA.get("SizeBytes");
				if (bytes == null) { // 第一个字节
					bytes = new byte[5];
					bytes[0] = 1;
					bytes[1] = (byte)readByte;
					DA.put("SizeBytes", bytes);
				}
				else {
					bytes[0]++;
					bytes[bytes[0]] = (byte)readByte;
					if (bytes[0] == 4) {
						int size = ((bytes[1]&0xFF)<<24)+((bytes[2]&0xFF)<<16)+((bytes[3]&0xFF)<<8)+(bytes[4]&0xFF);
						DA.put("ResponseSize", new Integer(size));
						DA.put("ReadByteNum", new Integer(4));
						DA.remove("SizeBytes");
BinTool.record("C:\\Receive.txt", ("Size:"+size).getBytes(), null, true);
					}
				}
			}
//if (bytes != null)
//	BinTool.record("C:\\Receive.txt", ("bytes:"+bytes[0]+' '+bytes[1]+' '+bytes[2]+' '+bytes[3]+' '+bytes[4]).getBytes(), null, true);
		}
		catch (Exception e) {
			BinTool.record("C:\\Exception.txt", e);
		}
		/* In case of success: */
		

		/* In case of exception: */
		// receiveAction.handleException(exception);
		// return true;
		//
		/* In case of further bytes needed to make the decision: */
		return false;
	}
}
