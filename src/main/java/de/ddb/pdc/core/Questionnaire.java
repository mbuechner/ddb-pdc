package de.ddb.pdc.core;

import de.ddb.pdc.metadata.DDBItem;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A Questionnaire to decide the public domain problem.
 *
 * This Questionnaire is given to the user by the public domain calculator
 * implementation. It behaves similar to an iterator and provides the
 * hasNextQuestion, getQuestion() and answerQuestion() methods to do the
 * questionnaire. If all questions have been answered the public domain problem
 * can be solved by calling isPublicDomain(). Call the getTrace method to get a
 * list of answered questions with the answers given to them.
 *
 * A Questionnaire is a stateful object. Do not reuse the same questionnaire
 * object for different cultural goods.
 */
public class Questionnaire {

  private FlowChartState state;
  private final List<AnsweredQuestion> answeredQuestions;
  private boolean isAborted;
  private final DDBItem metadata;

  /**
   * Creates a new Questionnaire from the initial state of the flow chart given
   * as parameter.
   *
   * @param initialState The initial state of the flow chart to use.
   * @param metadata     The DDBItem with the meta data about the cultural good
   */
  public Questionnaire(FlowChartState initialState, DDBItem metadata) {
    this.state = initialState;
    this.answeredQuestions = new LinkedList<AnsweredQuestion>();
    this.isAborted = false;
    this.metadata = metadata;
  }

  /**
   * Are there more questions to answer? This method returns true if there are
   * more questions in this questionnaire that need to be answered.
   *
   * @return true if more questions need to be answered
   */
  public boolean hasQuestionsLeft() {
    if (this.isAborted) {
      return false;
    }
    try {
      this.state.getQuestion();
      return true;
    } catch (NoSuchElementException e) {
      return false;
    }
  }

  /**
   * Get the current question.
   *
   * This function returns the question that needs to be answered. If no
   * questions is available to be answered a NoSuchElementException will be
   * thrown. Use the method hasNextQuestion to avoid this exception.
   *
   * @return the current question
   * @throws NoSuchElementException There is no current question
   */
  public Question getCurrentQuestion() throws NoSuchElementException {
    return this.state.getQuestion();
  }

  /**
   * Answer the current question and go to the next question. Call this method
   * to answer the current question. Everytime you answer a question the next
   * question will become the new current question. Get the current question by
   * calling the getCurrentQuestion method. If there are no questions to answer
   * (i.e. you have already answered all questions) a NoSuchElementException
   * will be thrown because there is no question to answer.
   *
   * @param answer The answer to the current question
   * @param assumption Assumptions made to answer the question
   * @throws NoSuchElementException There is no question that you can answer
   */
  public void answerCurrentQuestion(Answer answer, String assumption)
      throws NoSuchElementException {

    try {
      Question question = this.state.getQuestion();
      this.answeredQuestions.add(
          new AnsweredQuestion(question, answer, assumption));

      if (answer == Answer.UNKNOWN) {
        this.isAborted = true;
      } else {
        this.state = this.state.getNextState(answer);
      }
    } catch (IllegalStateException e) {
      throw new NoSuchElementException();
    }
  }

  /**
   * Decide the public domain problem. Try to return true if the cultural good
   * this questionnaire was answered for should fall into the public domain.
   * Return false if it does not. When information is missing because you did
   * not answer all questions, an IllegalStateException will be thrown. Make
   * sure that hasNextQuestion returns false before calling this method.
   *
   * @return true if the good falls into the public domain, false if it does
   *         not, and null if the decision can not be made.
   * @throws IllegalStateException You did not answer all the questions that are
   *         necessary
   */
  public Boolean isPublicDomain() throws IllegalStateException {
    if (this.isAborted) {
      return null;
    }
    if (this.state.hasResult()) {
      try {
        return this.state.getResult();
      } catch (CannotCalculateException e) {
        return null;
      }
    }
    throw new IllegalStateException();
  }

  /**
   * Returns a list of answered questions. This method will return a list
   * containing all the questions that have been asked and the response that was
   * given to answer them.
   *
   * @return List of answered questions
   */
  public List<AnsweredQuestion> getTrace() {
    return this.answeredQuestions;
  }

  /**
   * Get the result of this questionnaire. The result will contain the public
   * domain status of the cultural good this questionnaire was answered for,
   * the trace of all answered questions and the metadata of the cultural good.
   *
   * @return The public domain status and the answered question trace.
   * @throws IllegalStateException You did not answer all the questions that are
   *         necessary. You can not get a result yet.
   */
  public PDCResult getResult() throws IllegalStateException {
    PDCResult result = new PDCResult(
        this.isPublicDomain(), this.getTrace(), this.metadata
    );
    return result;
  }
}
