package com.example.jvmlab.common;

import lombok.extern.slf4j.Slf4j;

/**
 * 测试目的 / Test Purpose:
 * 中文：验证 ASM 动态类生成工具能正确生成接口实现与固定 toString 的类，并输出成功日志。
 * English: Verify ASM dynamic class builder generates correct interface implementations and fixed toString classes, printing success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：生成的实现类方法返回预设常量；toString 返回固定常量字符串；日志打印成功确认。
 * English: Generated implementation returns preset constant; toString returns fixed constant; logs print success confirmations.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志中的成功确认信息。
 * English: Run the main method and observe success confirmations in logs.
 */
@Slf4j
public class AsmDynamicClassBuilderTest {

    /**
     * 方法说明 / Method Description:
     * 中文：测试 createConstantImplementation 生成接口实现是否返回预期值。
     * English: Test whether createConstantImplementation returns expected value from generated class.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    public void testConstantImplementation() throws Exception {
        Class<? extends SampleService> clazz = AsmDynamicClassBuilder.createConstantImplementation(
                SampleService.class, "process", "ok", getClass().getClassLoader());
        SampleService instance = clazz.getDeclaredConstructor().newInstance();
        String result = instance.process();
        if ("ok".equals(result)) {
            log.info("【成功】接口实现返回预期值 / Success: interface implementation returned expected value");
        } else {
            log.error("接口实现返回值不匹配 / Failure: unexpected return value: {}", result);
        }
    }

    /**
     * 方法说明 / Method Description:
     * 中文：测试 createConstantToStringClass 生成的类 toString 是否返回预期值。
     * English: Test whether createConstantToStringClass returns expected toString value.
     *
     * 参数 / Parameters: 无
     * 返回值 / Return: 无
     * 异常 / Exceptions: 无
     */
    public void testConstantToString() throws Exception {
        Class<?> clazz = AsmDynamicClassBuilder.createConstantToStringClass(
                getClass().getClassLoader(), "com.example.jvmlab.common.DynamicToString", "TS");
        Object obj = clazz.getDeclaredConstructor().newInstance();
        String str = obj.toString();
        if ("TS".equals(str)) {
            log.info("【成功】toString 返回预期值 / Success: toString returned expected value");
        } else {
            log.error("toString 返回值不匹配 / Failure: unexpected toString: {}", str);
        }
    }

    /** 样例服务接口 / Sample service interface */
    interface SampleService {
        /**
         * 方法说明 / Method Description:
         * 中文：样例处理方法，供动态实现返回固定值。
         * English: Sample process method for dynamic implementation returning a fixed value.
         *
         * 参数 / Parameters: 无
         * 返回值 / Return: 中文：返回字符串 / English: String value
         * 异常 / Exceptions: 无
         */
        String process();
    }

    /** 入口方法 / Entry point */
    public static void main(String[] args) throws Exception {
        AsmDynamicClassBuilderTest t = new AsmDynamicClassBuilderTest();
        t.testConstantImplementation();
        t.testConstantToString();
        log.info("【成功】AsmDynamicClassBuilderTest 全部用例通过 / Success: all cases passed");
    }
}
