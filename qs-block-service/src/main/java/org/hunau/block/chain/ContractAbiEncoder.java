package org.hunau.block.chain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 智能合约ABI编码工具类
 * 用于编码合约函数调用数据
 */
public class ContractAbiEncoder {

    private static final Logger log = LoggerFactory.getLogger(ContractAbiEncoder.class);

    /**
     * 编码save函数调用
     * 合约方法签名：save(string businessKey, string proofType, string hash, string payload)
     */
    public static String encodeSave(String businessKey, String proofType, String hash, String payload) {
        List<org.web3j.abi.datatypes.Type> inputParameters = Arrays.asList(
                new Utf8String(businessKey),
                new Utf8String(proofType),
                new Utf8String(hash),
                new Utf8String(payload)
        );

        Function function = new Function(
                "save",
                inputParameters,
                Collections.emptyList()
        );

        String encoded = FunctionEncoder.encode(function);
        log.debug("编码save函数调用: businessKey={}, encodedLength={}", businessKey, encoded.length());
        return encoded;
    }

    /**
     * 编码verify函数调用
     * 合约方法签名：verify(string businessKey, string hash)
     */
    public static String encodeVerify(String businessKey, String hash) {
        List<org.web3j.abi.datatypes.Type> inputParameters = Arrays.asList(
                new Utf8String(businessKey),
                new Utf8String(hash)
        );

        Function function = new Function(
                "verify",
                inputParameters,
                Arrays.asList(new TypeReference<org.web3j.abi.datatypes.Bool>() {})
        );

        String encoded = FunctionEncoder.encode(function);
        log.debug("编码verify函数调用: businessKey={}", businessKey);
        return encoded;
    }

    /**
     * 解码bool类型返回值
     */
    public static boolean decodeBoolResult(String hexResult) {
        if (hexResult == null || hexResult.length() < 2) {
            return false;
        }
        // Solidity bool返回值：0x00表示false，其他表示true
        String cleanResult = hexResult.startsWith("0x") ? hexResult.substring(2) : hexResult;
        if (cleanResult.length() == 0) {
            return false;
        }
        // 取最后32字节
        String last64Chars = cleanResult.length() >= 64 ?
                cleanResult.substring(cleanResult.length() - 64) : cleanResult;
        // 非零表示true
        return !last64Chars.matches("0{64}");
    }
}
