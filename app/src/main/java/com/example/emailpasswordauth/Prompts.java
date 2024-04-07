package com.example.emailpasswordauth;

import java.util.HashMap;
import java.util.Map;

public class Prompts {
    public static final Map<String, String> possiblePrompts = new HashMap<>();

    static {
        possiblePrompts.put("STRESS", "How are your stress levels?");
        possiblePrompts.put("GRATEFUL", "How grateful are you?");
        possiblePrompts.put("CALM", "How calm are you?");
        possiblePrompts.put("HAPPY", "How happy do you feel?");
        possiblePrompts.put("CONNECTED", "How connected do you feel to those around you?");
        possiblePrompts.put("BALANCED", "How balanced is your life?");
        possiblePrompts.put("INTERESTS", "How well are you making time to pursue your interests?");
        possiblePrompts.put("GROUNDED", "How grounded do you feel in your daily life?");
        possiblePrompts.put("CHALLENGES", "How well are you handling life's challenges?");
        possiblePrompts.put("SATISFIED", "How satisfied does you lifestyle make you feel?");
    }

}
