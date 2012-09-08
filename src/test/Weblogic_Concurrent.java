package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import util.BinTool;

import com.ibm.rational.test.lt.kernel.services.ITestExecutionServices;
import com.ibm.rational.test.lt.execution.socket.custom.ISckCustomSend;
import com.ibm.rational.test.lt.execution.socket.custom.ISckSendAction;

/**
 *  Custom send <b>CustomSend_sebl_ins_WL</b>.<br>
 *  For javadoc of ITestExecutionServices, select 'Help Contents' in the Help menu and select
 *  'Extending Rational Performance Tester functionality' -> 'Extending test execution with custom code'
 */
public class Weblogic_Concurrent implements ISckCustomSend {

	// static {
	//	 static blocks are called once on each run and allow for example to bind
	//	 to an external dynamic library
	// }

	ISckSendAction sendAction;
	ITestExecutionServices testExecutionServices;
	
	static boolean DEBUG = false;
	static int N = 0;

	public Weblogic_Concurrent() {
		// The constructor is called during the test creation, not at the time of the execution of
		// the customized send action
	}

	public void setup(ITestExecutionServices tesRef,
			ISckSendAction sendActionRef) {
		testExecutionServices = tesRef;
		sendAction = sendActionRef;
		/* Do here any custom setup */

	}

	public byte[] onBeforeSubstitution(byte[] data) {
		/* Make here any changes to the data to be sent before substitutions are applied, if any */
		
		if (DEBUG)
			BinTool.record("E:\\request_"+(++N)+".bin", data);

		byte[] newdata = data;
		try {
			newdata = BinTool.modifyStream(data);
		} catch (Exception e) {
			BinTool.record("C:\\Exception.txt", e);
		}

		if (DEBUG && newdata!=data)
			BinTool.record("E:\\request_"+(++N)+".new.bin", newdata);
		return newdata;
	}

	public byte[] onAfterSubstitution(byte[] data) {
		/* Make here any changes to the data to be sent after substitutions are applied, if any */
		
		return data;
	}
}
