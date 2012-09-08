package test;

import util.BinTool;

import com.ibm.rational.test.lt.kernel.services.ITestExecutionServices;
import com.ibm.rational.test.lt.execution.socket.custom.ISckCustomSend;
import com.ibm.rational.test.lt.execution.socket.custom.ISckSendAction;

/**
 *  Custom send <b>Dump</b>.<br>
 *  For javadoc of ITestExecutionServices, select 'Help Contents' in the Help menu and select
 *  'Extending Rational Performance Tester functionality' -> 'Extending test execution with custom code'
 */
public class Dump implements ISckCustomSend {

	// static {
	//	 static blocks are called once on each run and allow for example to bind
	//	 to an external dynamic library
	// }

	ISckSendAction sendAction;
	ITestExecutionServices testExecutionServices;

	public Dump() {
		// The constructor is called during the test creation, not at the time of the execution of
		// the customized send action
	}

	public void setup(ITestExecutionServices tesRef,
			ISckSendAction sendActionRef) {
		testExecutionServices = tesRef;
		sendAction = sendActionRef;
		/* Do here any custom setup */

	}

	static int N = 1;
	public byte[] onBeforeSubstitution(byte[] data) {
		/* Make here any changes to the data to be sent before substitutions are applied, if any */
		BinTool.record("E:\\sebooking_insert"+N+++".bin", data);

		return data;
	}

	public byte[] onAfterSubstitution(byte[] data) {
		/* Make here any changes to the data to be sent after substitutions are applied, if any */

		return data;
	}
}
