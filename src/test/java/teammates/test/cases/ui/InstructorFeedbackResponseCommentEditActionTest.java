package teammates.test.cases.ui;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.FeedbackSessionAttributes;
import teammates.common.datatransfer.InstructorAttributes;
import teammates.common.util.Const;
import teammates.storage.api.FeedbackResponseCommentsDb;
import teammates.ui.controller.InstructorFeedbackResponseCommentEditAction;
import teammates.ui.controller.RedirectResult;

public class InstructorFeedbackResponseCommentEditActionTest extends
		BaseActionTest {
	DataBundle dataBundle;

	@BeforeClass
	public static void classSetUp() throws Exception {
		printTestClassHeader();
		uri = Const.ActionURIs.INSTRUCTOR_FEEDBACK_RESPONSE_COMMENT_EDIT;
	}

	@BeforeMethod
	public void caseSetUp() throws Exception {
		dataBundle = getTypicalDataBundle();
		restoreTypicalDataInDatastore();
	}

	@Test
	public void testAccessControl() throws Exception {
		FeedbackSessionAttributes fs = dataBundle.feedbackSessions.get("session1InCourse1");
		
		String[] submissionParams = new String[]{
				Const.ParamsNames.COURSE_ID, fs.courseId,
				Const.ParamsNames.FEEDBACK_SESSION_NAME, fs.feedbackSessionName,
				Const.ParamsNames.FEEDBACK_RESPONSE_COMMENT_ID, "",
				Const.ParamsNames.FEEDBACK_RESPONSE_COMMENT_TEXT, "",
				Const.ParamsNames.FEEDBACK_RESULTS_SORTTYPE, "recipient"
		};
		verifyOnlyInstructorsCanAccess(submissionParams);
	}
	
	@Test
	public void testExcecuteAndPostProcess() throws Exception {
		FeedbackResponseCommentsDb frcDb = new FeedbackResponseCommentsDb();

		FeedbackResponseCommentAttributes frc = dataBundle.feedbackResponseComments
				.get("comment1FromT1C1ToR1Q1S1C1");
		frc = frcDb.getFeedbackResponseComment(frc.feedbackResponseId,
				frc.giverEmail, frc.createdAt);
		assertNotNull("response comment not found", frc);
		
		InstructorAttributes instructor = dataBundle.instructors.get("instructor1OfCourse1");
		gaeSimulation.loginAsInstructor(instructor.googleId);
		
		______TS("not enough parameters");
		
		verifyAssumptionFailure();
		
		String[] submissionParams = new String[]{
				Const.ParamsNames.COURSE_ID, frc.courseId,
				Const.ParamsNames.FEEDBACK_SESSION_NAME, frc.feedbackSessionName,
				Const.ParamsNames.FEEDBACK_RESPONSE_COMMENT_TEXT, "Comment to first response",
				Const.ParamsNames.USER_ID, instructor.googleId
		};
		
		verifyAssumptionFailure(submissionParams);
		
		______TS("typical case");
		
		submissionParams = new String[]{
				Const.ParamsNames.COURSE_ID, frc.courseId,
				Const.ParamsNames.FEEDBACK_SESSION_NAME, frc.feedbackSessionName,
				Const.ParamsNames.FEEDBACK_RESPONSE_COMMENT_ID, frc.getId().toString(),
				Const.ParamsNames.FEEDBACK_RESPONSE_COMMENT_TEXT, frc.commentText + " (Edited)",
				Const.ParamsNames.FEEDBACK_RESULTS_SORTTYPE, "recipient"
		};
		
		InstructorFeedbackResponseCommentEditAction a = getAction(submissionParams);
		RedirectResult rr = (RedirectResult) a.executeAndPostProcess();
		
		assertEquals(Const.ActionURIs.INSTRUCTOR_FEEDBACK_RESULTS_PAGE 
				+ "?courseid=idOfTypicalCourse1&fsname=First+feedback+session"
				+ "&user=idOfInstructor1OfCourse1&frsorttype=recipient"
				+ "&message=Your+changes+has+been+saved+successfully&error=false",
				rr.getDestinationWithParams());
		assertFalse(rr.isError);
		assertEquals(Const.StatusMessages.FEEDBACK_RESPONSE_COMMENT_EDITED, rr.getStatusMessage());
		
		______TS("empty comment text");
		
		submissionParams = new String[]{
				Const.ParamsNames.COURSE_ID, frc.courseId,
				Const.ParamsNames.FEEDBACK_SESSION_NAME, frc.feedbackSessionName,
				Const.ParamsNames.FEEDBACK_RESPONSE_COMMENT_ID, frc.getId().toString(),
				Const.ParamsNames.FEEDBACK_RESPONSE_COMMENT_TEXT, "",
				Const.ParamsNames.FEEDBACK_RESULTS_SORTTYPE, "recipient"
		};

		a = getAction(submissionParams);
		rr = (RedirectResult) a.executeAndPostProcess();
		
		assertEquals(Const.ActionURIs.INSTRUCTOR_FEEDBACK_RESULTS_PAGE 
				+ "?courseid=idOfTypicalCourse1&fsname=First+feedback+session"
				+ "&user=idOfInstructor1OfCourse1&frsorttype=recipient"
				+ "&message=Comment+cannot+be+empty&error=true",
				rr.getDestinationWithParams());
		assertTrue(rr.isError);
		assertEquals(Const.StatusMessages.FEEDBACK_RESPONSE_COMMENT_EMPTY, rr.getStatusMessage());
	}
	
	private InstructorFeedbackResponseCommentEditAction getAction(String... params) throws Exception {
		return (InstructorFeedbackResponseCommentEditAction) (gaeSimulation.getActionObject(uri, params));
	}
}
