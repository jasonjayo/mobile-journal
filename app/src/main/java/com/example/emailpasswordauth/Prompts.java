package com.example.emailpasswordauth;

import java.util.HashMap;
import java.util.Map;

public class Prompts {
    public static final Map<String, String> possiblePrompts = new HashMap<>();
    public static final Map<String, String> possibleQuestions = new HashMap<>();

    static {
        // Prompts
        possiblePrompts.put("STRESS", "How are your stress levels? \uD83D\uDE1F");
        possiblePrompts.put("GRATEFUL", "How grateful are you? 😊");
        possiblePrompts.put("CALM", "How calm are you? ☺");
        possiblePrompts.put("HAPPY", "How happy do you feel? 😁");
        possiblePrompts.put("CONNECTED", "How connected do you feel to those around you? \uD83E\uDD1D");
        possiblePrompts.put("BALANCED", "How balanced is your life? ⚖");
        possiblePrompts.put("INTERESTS", "How well are you making time to pursue your interests? 🎨");
        possiblePrompts.put("GROUNDED", "How grounded do you feel in your daily life? 🌱");
        possiblePrompts.put("CHALLENGES", "How well are you handling life's challenges? 🥊");
        possiblePrompts.put("SATISFIED", "How satisfied does your lifestyle make you feel? 😅");

        // Notification Questions
        possibleQuestions.put("FEELING", "How are you feeling today?");
        possibleQuestions.put("READY", "Are you ready to use the journal?");
        possibleQuestions.put("MOOD", "What's your mood like right now?");
        possibleQuestions.put("SMILE", "What's something that made you smile recently?");
        possibleQuestions.put("MIND", "What's on your mind at the moment?");
        possibleQuestions.put("REFLECT", "Is there anything you'd like to reflect on today?");
        possibleQuestions.put("GOAL", "What's one small goal you'd like to achieve today?");
        possibleQuestions.put("SELF_CARE", "How can you practice self-care today?");
        possibleQuestions.put("AFFIRMATION", "What's a positive affirmation you can tell yourself right now?");
        possibleQuestions.put("LEARNED", "What's one thing you learned today?");
        possibleQuestions.put("POSITIVE_EXPERIENCE", "Describe one positive experience you had today.");
        possibleQuestions.put("HIGHLIGHT", "What was the highlight of your day?");
    }
}
