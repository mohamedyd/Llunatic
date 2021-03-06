package it.unibas.lunatic.persistence.encoding;

import it.unibas.lunatic.exceptions.DAOException;
import it.unibas.lunatic.model.chase.commons.ChaseStats;
import java.util.Date;
import java.util.Properties;
import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.engine.control.CompositeCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import speedy.SpeedyConstants;
import speedy.model.database.operators.dbms.IValueEncoder;
import speedy.utility.SpeedyUtility;

public class JSCDictionaryEncoder implements IValueEncoder {

    private final static Logger logger = LoggerFactory.getLogger(JSCDictionaryEncoder.class);
    private long lastValue = 0;
    private CompositeCacheManager ccm;
    private CacheAccess<String, Long> encodingCache;
    private CacheAccess<Long, String> decodingCache;

    public JSCDictionaryEncoder(String scenarioName) {
        this.initCaches(scenarioName);
    }

    public String encode(String original) {
        long start = new Date().getTime();
        Long encoded = encodingCache.get(original);
        if (encoded == null) {
            encoded = nextValue();
            encodingCache.put(original, encoded);
            decodingCache.put(encoded, original);
        }
        long end = new Date().getTime();
        ChaseStats.getInstance().addStat(ChaseStats.DICTIONARY_ENCODING_TIME, end - start);
        return encoded + "";
    }

    public String decode(String encoded) {
        long start = new Date().getTime();
        String decodedValue = decodeValueUsingCache(encoded);
        long end = new Date().getTime();
        ChaseStats.getInstance().addStat(ChaseStats.DICTIONARY_DECODING_TIME, end - start);
        return decodedValue;
    }

    public void removeExistingEncoding() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    private String decodeValueUsingCache(String encoded) {
        Long encodedValue;
        try {
            encodedValue = Long.parseLong(encoded);
        } catch (NumberFormatException nfe) {
            throw new DAOException("Unable to decode string value " + encoded);
        }
        String decoded = decodingCache.get(encodedValue);
        if (decoded == null) {
            if (SpeedyUtility.isSkolem(encoded) || SpeedyUtility.isVariable(encoded)) {
                return encoded;
            }
            throw new DAOException("Unable to decode value " + encodedValue + ". Cache stats:\n" + decodingCache.getStats());
        }
        return decoded;
    }

    private Long nextValue() {
        lastValue++;
        if (lastValue < SpeedyConstants.MIN_BIGINT_SKOLEM_VALUE && lastValue < SpeedyConstants.MIN_BIGINT_LLUN_VALUE) {
            return lastValue;
        }
        String stringLastValue = lastValue + "";
        if (stringLastValue.startsWith(SpeedyConstants.BIGINT_SKOLEM_PREFIX) || stringLastValue.startsWith(SpeedyConstants.BIGINT_LLUN_PREFIX)) {
            lastValue += SpeedyConstants.MIN_BIGINT_SAFETY_SKIP_VALUE;
        }
        if (logger.isTraceEnabled()) {
            if (SpeedyUtility.isSkolem(lastValue + "") || SpeedyUtility.isVariable(lastValue + "")) throw new IllegalArgumentException("Dictionary encoder generates a skolem or variable " + lastValue);
            if (lastValue % 1000000L == 0) logger.trace("Next value: " + lastValue);
        }
        return lastValue;
    }

    private void initCaches(String scenarioName) {
        long start = new Date().getTime();
        Properties props = new Properties();
        this.ccm = CompositeCacheManager.getUnconfiguredInstance();
        // Region cache
        props.put("jcs.region.dictionarycache_enc_" + scenarioName, "DICTIONARY_ENC_" + scenarioName);
        props.put("jcs.region.dictionarycache_enc" + scenarioName + ".cacheattributes", "org.apache.jcs.engine.CompositeCacheAttributes");
        props.put("jcs.region.dictionarycache_enc" + scenarioName + ".cacheattributes.MaxObjects", "100000000");
        props.put("jcs.region.dictionarycache_enc" + scenarioName + ".cacheattributes.MemoryCacheName", "org.apache.jcs.engine.memory.lru.LRUMemoryCache");
        props.put("jcs.region.dictionarycache_enc" + scenarioName + ".cacheattributes.UseMemoryShrinker", "true");
        props.put("jcs.region.dictionarycache_enc" + scenarioName + ".cacheattributes.MaxMemoryIdleTimeSeconds", "3600");
        props.put("jcs.region.dictionarycache_enc" + scenarioName + ".cacheattributes.ShrinkerIntervalSeconds", "60");
        props.put("jcs.region.dictionarycache_enc" + scenarioName + ".cacheattributes.MaxSpoolPerRun", "500");
        props.put("jcs.region.dictionarycache_enc" + scenarioName + ".cacheattributes.DiskUsagePatternName", "UPDATE");
        props.put("jcs.region.dictionarycache_enc" + scenarioName + ".elementattributes", "org.apache.jcs.engine.ElementAttributes");
        props.put("jcs.region.dictionarycache_enc" + scenarioName + ".elementattributes.IsEternal", "true");
        // Auxiliary
        props.put("jcs.auxiliary.DICTIONARY_ENC_" + scenarioName, "org.apache.commons.jcs.auxiliary.disk.indexed.IndexedDiskCacheFactory");
        props.put("jcs.auxiliary.DICTIONARY_ENC_" + scenarioName + ".attributes", "org.apache.commons.jcs.auxiliary.disk.indexed.IndexedDiskCacheAttributes");
        props.put("jcs.auxiliary.DICTIONARY_ENC_" + scenarioName + ".attributes.DiskPath", "${user.home}/Temp/JCS/Dictionary/" + scenarioName + "/");
        props.put("jcs.auxiliary.DICTIONARY_ENC_" + scenarioName + ".attributes.MaxPurgatorySize", "10000");
        props.put("jcs.auxiliary.DICTIONARY_ENC_" + scenarioName + ".attributes.MaxKeySize", "-1");
        props.put("jcs.auxiliary.DICTIONARY_ENC_" + scenarioName + ".attributes.MaxRecycleBinSize", "7500");
        props.put("jcs.auxiliary.DICTIONARY_ENC_" + scenarioName + ".attributes.ClearDiskOnStartup", "false");
        props.put("jcs.auxiliary.DICTIONARY_ENC_" + scenarioName + ".attributes.OptimizeOnShutdown", "false");
        // Region cache
        props.put("jcs.region.dictionarycache_dec_" + scenarioName, "DICTIONARY_DEC_" + scenarioName);
        props.put("jcs.region.dictionarycache_dec_" + scenarioName + ".cacheattributes", "org.apache.jcs.engine.CompositeCacheAttributes");
        props.put("jcs.region.dictionarycache_dec_" + scenarioName + ".cacheattributes.MaxObjects", "100000000");
        props.put("jcs.region.dictionarycache_dec_" + scenarioName + ".cacheattributes.MemoryCacheName", "org.apache.jcs.engine.memory.lru.LRUMemoryCache");
        props.put("jcs.region.dictionarycache_dec_" + scenarioName + ".cacheattributes.UseMemoryShrinker", "true");
        props.put("jcs.region.dictionarycache_dec_" + scenarioName + ".cacheattributes.MaxMemoryIdleTimeSeconds", "3600");
        props.put("jcs.region.dictionarycache_dec_" + scenarioName + ".cacheattributes.ShrinkerIntervalSeconds", "60");
        props.put("jcs.region.dictionarycache_dec_" + scenarioName + ".cacheattributes.MaxSpoolPerRun", "500");
        props.put("jcs.region.dictionarycache_dec_" + scenarioName + ".cacheattributes.DiskUsagePatternName", "UPDATE");
        props.put("jcs.region.dictionarycache_dec_" + scenarioName + ".elementattributes", "org.apache.jcs.engine.ElementAttributes");
        props.put("jcs.region.dictionarycache_dec_" + scenarioName + ".elementattributes.IsEternal", "true");
        // Auxiliary
        props.put("jcs.auxiliary.DICTIONARY_DEC_" + scenarioName, "org.apache.commons.jcs.auxiliary.disk.indexed.IndexedDiskCacheFactory");
        props.put("jcs.auxiliary.DICTIONARY_DEC_" + scenarioName + ".attributes", "org.apache.commons.jcs.auxiliary.disk.indexed.IndexedDiskCacheAttributes");
        props.put("jcs.auxiliary.DICTIONARY_DEC_" + scenarioName + ".attributes.DiskPath", "${user.home}/Temp/JCS/Dictionary/" + scenarioName + "/");
        props.put("jcs.auxiliary.DICTIONARY_DEC_" + scenarioName + ".attributes.MaxPurgatorySize", "10000");
        props.put("jcs.auxiliary.DICTIONARY_DEC_" + scenarioName + ".attributes.MaxKeySize", "-1");
        props.put("jcs.auxiliary.DICTIONARY_DEC_" + scenarioName + ".attributes.MaxRecycleBinSize", "7500");
        props.put("jcs.auxiliary.DICTIONARY_DEC_" + scenarioName + ".attributes.ClearDiskOnStartup", "false");
        props.put("jcs.auxiliary.DICTIONARY_DEC_" + scenarioName + ".attributes.OptimizeOnShutdown", "false");
        // Configure
        ccm.configure(props);
        // Access region
        this.encodingCache = JCS.getInstance("dictionarycache_enc_" + scenarioName);
        this.decodingCache = JCS.getInstance("dictionarycache_dec_" + scenarioName);
        long end = new Date().getTime();
        ChaseStats.getInstance().addStat(ChaseStats.DICTIONARY_LOADING_TIME, end - start);
    }

    public void prepareForEncoding() {
        //Done in the constructor
    }

    public void closeEncoding() {
        //Nothing to do
    }

    public void prepareForDecoding() {
        //Done in the constructor
    }

    public void closeDecoding() {
        long start = new Date().getTime();
        this.ccm.shutDown();
        long end = new Date().getTime();
        ChaseStats.getInstance().addStat(ChaseStats.DICTIONARY_CLOSING_TIME, end - start);
    }

    public void waitingForEnding() {
    }

}
