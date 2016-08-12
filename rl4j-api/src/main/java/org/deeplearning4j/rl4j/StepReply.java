package org.deeplearning4j.rl4j;

import lombok.Value;
import org.json.JSONObject;

/**
 * @author rubenfiszel (ruben.fiszel@epfl.ch) on 7/6/16.
 *
 * StepReply is the container for the data returned after each step(action).
 *
 * @param <T> type of observation
 */
@Value
public class StepReply<T> {

    T observation;
    double reward;
    boolean done;
    JSONObject info;

}
