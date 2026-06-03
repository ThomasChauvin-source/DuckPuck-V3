package com.example.duckpuck;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class ReplayData {

    private ReplayData() {
    }

    public static class Frame {
        public long timeMs;
        public float puckX;
        public float puckY;
        public float[] malletX;
        public float[] malletY;

        public Frame(long timeMs, float puckX, float puckY, float[] malletX, float[] malletY) {
            this.timeMs = timeMs;
            this.puckX = puckX;
            this.puckY = puckY;
            this.malletX = malletX;
            this.malletY = malletY;
        }
    }

    public static class Goal {
        public int scorer;
        public int scoreRed;
        public int scoreBlue;
        public final List<Frame> frames = new ArrayList<>();
    }

    public static String toJson(List<Goal> goals) {
        if (goals == null || goals.isEmpty()) return "";

        JSONArray goalsArray = new JSONArray();
        try {
            for (Goal goal : goals) {
                JSONObject goalObject = new JSONObject();
                goalObject.put("scorer", goal.scorer);
                goalObject.put("scoreRed", goal.scoreRed);
                goalObject.put("scoreBlue", goal.scoreBlue);

                JSONArray framesArray = new JSONArray();
                for (Frame frame : goal.frames) {
                    JSONObject frameObject = new JSONObject();
                    frameObject.put("timeMs", frame.timeMs);
                    frameObject.put("puckX", frame.puckX);
                    frameObject.put("puckY", frame.puckY);
                    frameObject.put("malletX", floatsToJson(frame.malletX));
                    frameObject.put("malletY", floatsToJson(frame.malletY));
                    framesArray.put(frameObject);
                }

                goalObject.put("frames", framesArray);
                goalsArray.put(goalObject);
            }
        } catch (JSONException ignored) {
            return "";
        }
        return goalsArray.toString();
    }

    public static List<Goal> fromJson(String json) {
        List<Goal> goals = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return goals;

        try {
            JSONArray goalsArray = new JSONArray(json);
            for (int i = 0; i < goalsArray.length(); i++) {
                JSONObject goalObject = goalsArray.getJSONObject(i);
                Goal goal = new Goal();
                goal.scorer = goalObject.optInt("scorer", 0);
                goal.scoreRed = goalObject.optInt("scoreRed", 0);
                goal.scoreBlue = goalObject.optInt("scoreBlue", 0);

                JSONArray framesArray = goalObject.optJSONArray("frames");
                if (framesArray != null) {
                    for (int j = 0; j < framesArray.length(); j++) {
                        JSONObject frameObject = framesArray.getJSONObject(j);
                        goal.frames.add(new Frame(
                                frameObject.optLong("timeMs", 0L),
                                (float) frameObject.optDouble("puckX", 0.5),
                                (float) frameObject.optDouble("puckY", 0.5),
                                jsonToFloats(frameObject.optJSONArray("malletX")),
                                jsonToFloats(frameObject.optJSONArray("malletY"))
                        ));
                    }
                }

                if (!goal.frames.isEmpty()) {
                    goals.add(goal);
                }
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }

        return goals;
    }

    private static JSONArray floatsToJson(float[] values) throws JSONException {
        JSONArray array = new JSONArray();
        if (values == null) return array;
        for (float value : values) {
            array.put(value);
        }
        return array;
    }

    private static float[] jsonToFloats(JSONArray array) {
        if (array == null) return new float[0];
        float[] values = new float[array.length()];
        for (int i = 0; i < array.length(); i++) {
            values[i] = (float) array.optDouble(i, 0.0);
        }
        return values;
    }
}
