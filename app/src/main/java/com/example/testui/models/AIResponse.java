package com.example.testui.models;

import java.util.List;

public class AIResponse {
    private String subject;
    private String logicalSteps;
    private String finalAnswer;
    private String simplifiedExplanation;
    private List<String> formulas;
    private List<String> alternatives;
    private List<String> commonMistakes;

    public AIResponse(String subject, String logicalSteps, String finalAnswer, String simplifiedExplanation) {
        this.subject = subject;
        this.logicalSteps = logicalSteps;
        this.finalAnswer = finalAnswer;
        this.simplifiedExplanation = simplifiedExplanation;
    }

    // Getters and Setters
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getLogicalSteps() { return logicalSteps; }
    public void setLogicalSteps(String logicalSteps) { this.logicalSteps = logicalSteps; }

    public String getFinalAnswer() { return finalAnswer; }
    public void setFinalAnswer(String finalAnswer) { this.finalAnswer = finalAnswer; }

    public String getSimplifiedExplanation() { return simplifiedExplanation; }
    public void setSimplifiedExplanation(String simplifiedExplanation) { this.simplifiedExplanation = simplifiedExplanation; }

    public List<String> getFormulas() { return formulas; }
    public void setFormulas(List<String> formulas) { this.formulas = formulas; }

    public List<String> getAlternatives() { return alternatives; }
    public void setAlternatives(List<String> alternatives) { this.alternatives = alternatives; }

    public List<String> getCommonMistakes() { return commonMistakes; }
    public void setCommonMistakes(List<String> commonMistakes) { this.commonMistakes = commonMistakes; }
}
