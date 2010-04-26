/**
 * Copyright 2009 Takahiko Ito
 * 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0 
 *        
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package org.unigram.likelike.lsh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.unigram.likelike.common.Candidate;
import org.unigram.likelike.common.LikelikeConstants;

/**
 * Reducer implementation. Extract pairs related to each other.
 */
public class GetRecommendationsReducer extends
        Reducer<LongWritable, Candidate, 
        LongWritable, LongWritable> {

    /**
     * reduce. 
     * @param key target
     * @param values candidates
     * @param context -
     * @throws IOException - 
     * @throws InterruptedException -
     */
    public void reduce(final LongWritable key,
            final Iterable<Candidate> values,
            final Context context)
            throws IOException, InterruptedException {
        
        HashMap<Long, Double> candidates 
            = new HashMap<Long, Double>();
        for (Candidate cand : values) {
            Long tid = cand.getId().get();
            if (candidates.containsKey(tid)) {
               Double weight = candidates.get(tid);
               weight += 1.0;
               candidates.put(tid, weight);
            } else {
                candidates.put(tid, 
                        new Double(1.0));
            }
            
            if (candidates.size() > 50000) { // TODO should be parameterized
                break;
            }
        }
        
        /* sort by value and then output */
        ArrayList<Map.Entry> array 
            = new ArrayList<Map.Entry>(candidates.entrySet());
        Collections.sort(array, new Comparator<Object>(){
            public int compare(final Object o1, final Object o2){
                Map.Entry e1 =(Map.Entry)o1;
                Map.Entry e2 =(Map.Entry)o2;
                Double e1Value = (Double) e1.getValue();
                Double e2Value = (Double) e2.getValue();
                return (e2Value.compareTo(e1Value));
            }
        });

        Iterator it = array.iterator();
        int i = 0;
        while(it.hasNext()) {
            if (i >= this.maxOutputSize) {
                return;
            }
            Map.Entry obj = (Map.Entry) it.next();
            context.write(key, new LongWritable((Long)obj.getKey()));
            i += 1;
        }
    }
    
    /** maximum number of output per example. */
    private long maxOutputSize;
    
    /**
     * setup.
     * 
     * @param context contains Configuration object to get settings
     */
    @Override
    public final void setup(final Context context) {
        Configuration jc = null; 
        if (context == null) {
            jc = new Configuration();
        } else {
            jc = context.getConfiguration();
        }
        this.maxOutputSize = jc.getLong(
                LikelikeConstants.MAX_OUTPUT_SIZE , 
                LikelikeConstants.DEFAULT_MAX_OUTPUT_SIZE);
    }    
}
