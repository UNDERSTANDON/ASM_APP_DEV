package com.example.testui;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.testui.ai.AIObj;
import com.example.testui.database.DatabaseHelper;
import com.example.testui.models.AIResponse;
import com.example.testui.models.ChatMessage;
import com.example.testui.models.QuizQuestion;
import com.google.android.material.button.MaterialButton;

import io.noties.markwon.Markwon;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;

    private final List<ChatMessage> messageList;
    private final SimpleDateFormat timeFormat;
    private Markwon markwon;

    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (markwon == null) {
            markwon = Markwon.create(parent.getContext());
        }
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_ai, parent, false);
            return new AIViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        String timeStr = timeFormat.format(new Date(message.getTimestamp()));

        if (holder instanceof UserViewHolder) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            markwon.setMarkdown(userHolder.textMessage, message.getText());
            userHolder.textTime.setText(timeStr);
        } else if (holder instanceof AIViewHolder) {
            AIViewHolder aiHolder = (AIViewHolder) holder;
            
            if (message.isThinking()) {
                // Show thinking progress and hide actual response content
                aiHolder.thinkingProgress.setVisibility(View.VISIBLE);
                aiHolder.textThinkingLabel.setVisibility(View.VISIBLE);
                aiHolder.textExplanation.setVisibility(View.GONE);
                aiHolder.layoutDetails.setVisibility(View.GONE);
                aiHolder.layoutQuizAction.setVisibility(View.GONE);
                aiHolder.textTime.setText("");
            } else {
                // Hide thinking progress and show content
                aiHolder.thinkingProgress.setVisibility(View.GONE);
                aiHolder.textThinkingLabel.setVisibility(View.GONE);
                aiHolder.textExplanation.setVisibility(View.VISIBLE);
                aiHolder.textTime.setText(timeStr);

                AIResponse response = message.getAiResponse();
                if (response != null) {
                    markwon.setMarkdown(aiHolder.textExplanation, response.getSimplifiedExplanation());
                    aiHolder.layoutDetails.setVisibility(View.VISIBLE);
                    aiHolder.layoutQuizAction.setVisibility(View.VISIBLE);

                    // Setup Quiz Action
                    aiHolder.btnGenerateQuiz.setOnClickListener(v -> {
                        long questionId = message.getQuestionId();
                        if (questionId == -1) {
                            Toast.makeText(v.getContext(), "Không tìm thấy ID câu hỏi để tạo quiz.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DatabaseHelper dbHelper = new DatabaseHelper(v.getContext());
                        List<QuizQuestion> existingQuiz = dbHelper.getQuizForQuestion(questionId);
                        if (!existingQuiz.isEmpty()) {
                            Intent intent = new Intent(v.getContext(), QuizActivity.class);
                            intent.putExtra("quiz_questions", (Serializable) existingQuiz);
                            v.getContext().startActivity(intent);
                            return;
                        }

                        String instruction = aiHolder.editQuizInstruction.getText().toString().trim();
                        aiHolder.btnGenerateQuiz.setEnabled(false);
                        aiHolder.btnGenerateQuiz.setText("Đang tạo...");

                        String context = "Question: " + message.getText() + "\nAnswer: " + response.getLogicalSteps();

                        AIObj.getInstance().generateQuiz(context, instruction, new AIObj.AICallback<List<QuizQuestion>>() {
                            @Override
                            public void onSuccess(List<QuizQuestion> result) {
                                aiHolder.btnGenerateQuiz.setEnabled(true);
                                aiHolder.btnGenerateQuiz.setText("Tạo bài kiểm tra");
                                
                                // Save to DB
                                dbHelper.saveQuiz(questionId, result);
                                
                                Intent intent = new Intent(v.getContext(), QuizActivity.class);
                                intent.putExtra("quiz_questions", (Serializable) result);
                                v.getContext().startActivity(intent);
                            }

                            @Override
                            public void onError(String error) {
                                aiHolder.btnGenerateQuiz.setEnabled(true);
                                aiHolder.btnGenerateQuiz.setText("Tạo bài kiểm tra");
                                Toast.makeText(v.getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });

                    // --- Process Logical Steps ---
                    String steps = response.getLogicalSteps();
                    if (steps != null && !steps.trim().isEmpty()) {
                        aiHolder.btnToggleSteps.setVisibility(View.VISIBLE);
                        markwon.setMarkdown(aiHolder.textSteps, steps);
                        // Setup collapse/expand
                        aiHolder.btnToggleSteps.setOnClickListener(v -> {
                            int visibility = aiHolder.layoutStepsContent.getVisibility();
                            if (visibility == View.VISIBLE) {
                                aiHolder.layoutStepsContent.setVisibility(View.GONE);
                                aiHolder.iconToggleSteps.setImageResource(R.drawable.ic_expand_more);
                            } else {
                                aiHolder.layoutStepsContent.setVisibility(View.VISIBLE);
                                aiHolder.iconToggleSteps.setImageResource(R.drawable.ic_close); // Change to a close or check icon
                            }
                        });
                    } else {
                        aiHolder.btnToggleSteps.setVisibility(View.GONE);
                        aiHolder.layoutStepsContent.setVisibility(View.GONE);
                    }

                    // --- Process Formulas ---
                    List<String> formulas = response.getFormulas();
                    if (formulas != null && !formulas.isEmpty()) {
                        aiHolder.layoutFormulas.setVisibility(View.VISIBLE);
                        StringBuilder sb = new StringBuilder();
                        for (String f : formulas) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append("* ").append(f);
                        }
                        markwon.setMarkdown(aiHolder.textFormulas, sb.toString());
                    } else {
                        aiHolder.layoutFormulas.setVisibility(View.GONE);
                    }

                    // --- Process Alternatives ---
                    List<String> alternatives = response.getAlternatives();
                    if (alternatives != null && !alternatives.isEmpty()) {
                        aiHolder.layoutAlternatives.setVisibility(View.VISIBLE);
                        StringBuilder sb = new StringBuilder();
                        for (String alt : alternatives) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append("* ").append(alt);
                        }
                        markwon.setMarkdown(aiHolder.textAlternatives, sb.toString());
                    } else {
                        aiHolder.layoutAlternatives.setVisibility(View.GONE);
                    }

                    // --- Process Common Mistakes ---
                    List<String> mistakes = response.getCommonMistakes();
                    if (mistakes != null && !mistakes.isEmpty()) {
                        aiHolder.layoutMistakes.setVisibility(View.VISIBLE);
                        StringBuilder sb = new StringBuilder();
                        for (String mis : mistakes) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append("* ").append(mis);
                        }
                        markwon.setMarkdown(aiHolder.textMistakes, sb.toString());
                    } else {
                        aiHolder.layoutMistakes.setVisibility(View.GONE);
                    }
                } else {
                    // Just simple text fallback
                    markwon.setMarkdown(aiHolder.textExplanation, message.getText());
                    aiHolder.layoutDetails.setVisibility(View.GONE);
                    aiHolder.layoutQuizAction.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView textTime;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_user_message);
            textTime = itemView.findViewById(R.id.text_user_time);
        }
    }

    public static class AIViewHolder extends RecyclerView.ViewHolder {
        ProgressBar thinkingProgress;
        TextView textThinkingLabel;
        TextView textExplanation;
        TextView textTime;

        LinearLayout layoutDetails;
        LinearLayout btnToggleSteps;
        ImageView iconToggleSteps;
        LinearLayout layoutStepsContent;
        TextView textSteps;

        LinearLayout layoutFormulas;
        TextView textFormulas;
        LinearLayout layoutAlternatives;
        TextView textAlternatives;
        LinearLayout layoutMistakes;
        TextView textMistakes;

        LinearLayout layoutQuizAction;
        EditText editQuizInstruction;
        MaterialButton btnGenerateQuiz;

        public AIViewHolder(@NonNull View itemView) {
            super(itemView);
            thinkingProgress = itemView.findViewById(R.id.thinking_progress);
            textThinkingLabel = itemView.findViewById(R.id.text_thinking_label);
            textExplanation = itemView.findViewById(R.id.text_ai_explanation);
            textTime = itemView.findViewById(R.id.text_ai_time);

            layoutDetails = itemView.findViewById(R.id.layout_ai_details);
            btnToggleSteps = itemView.findViewById(R.id.btn_toggle_steps);
            iconToggleSteps = itemView.findViewById(R.id.icon_toggle_steps);
            layoutStepsContent = itemView.findViewById(R.id.layout_steps_content);
            textSteps = itemView.findViewById(R.id.text_steps);

            layoutFormulas = itemView.findViewById(R.id.layout_formulas);
            textFormulas = itemView.findViewById(R.id.text_formulas);
            layoutAlternatives = itemView.findViewById(R.id.layout_alternatives);
            textAlternatives = itemView.findViewById(R.id.text_alternatives);
            layoutMistakes = itemView.findViewById(R.id.layout_mistakes);
            textMistakes = itemView.findViewById(R.id.text_mistakes);

            layoutQuizAction = itemView.findViewById(R.id.layout_quiz_action);
            editQuizInstruction = itemView.findViewById(R.id.edit_quiz_instruction);
            btnGenerateQuiz = itemView.findViewById(R.id.btn_generate_quiz);
        }
    }
}
