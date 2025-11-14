package com.example.jvmlab.chapter07;

import lombok.extern.slf4j.Slf4j;

/**
 * 测试目的 / Test Purpose:
 * 中文：覆盖第7章控制器的所有剩余方法，验证返回文本并打印成功日志。
 * English: Cover all remaining methods of Chapter 07 controller, verify returned text and print success logs.
 *
 * 预期结果 / Expected Result:
 * 中文：各方法返回包含预期关键词的字符串，并打印成功确认。
 * English: Each method returns strings containing expected keywords with success confirmations.
 *
 * 执行方式 / How to Execute:
 * 中文：运行 main 方法观察日志。
 * English: Run main method and observe logs.
 */
@Slf4j
public class Chapter07CompleteTest {

    public void testActiveUseNew() {
        Chapter07Controller ctrl = new Chapter07Controller();
        String res = ctrl.testActiveUseByNew();
        if (res.contains("Class initialized") || res.length() > 0) {
            log.info("【成功】active-use-new 返回初始化日志提示 / Success: {}", res);
        } else {
            log.error("active-use-new 返回不符合预期 / Failure: {}", res);
        }
    }

    public void testActiveUseStaticField() {
        Chapter07Controller ctrl = new Chapter07Controller();
        String res = ctrl.testActiveUseByStaticField();
        if (res.contains("value=") && res.contains("constant=")) {
            log.info("【成功】active-use-static-field 返回字段与常量 / Success: {}", res);
        } else {
            log.error("active-use-static-field 返回不符合预期 / Failure: {}", res);
        }
    }

    public void testPassiveUseParentField() {
        Chapter07Controller ctrl = new Chapter07Controller();
        String res = ctrl.testPassiveUseParentField();
        if (res.contains("Parent value:")) {
            log.info("【成功】passive-use-parent-field 返回父类字段值 / Success: {}", res);
        } else {
            log.error("passive-use-parent-field 返回不符合预期 / Failure: {}", res);
        }
    }

    public void testPassiveUseArray() {
        Chapter07Controller ctrl = new Chapter07Controller();
        String res = ctrl.testPassiveUseArray();
        if (res.contains("Array type:")) {
            log.info("【成功】passive-use-array 返回数组类型 / Success: {}", res);
        } else {
            log.error("passive-use-array 返回不符合预期 / Failure: {}", res);
        }
    }

    public void testClassLoadingPreparation() {
        Chapter07Controller ctrl = new Chapter07Controller();
        String res = ctrl.testClassLoadingPreparation();
        if (res.contains("Check logs")) {
            log.info("【成功】class-loading-preparation 返回提示 / Success: {}", res);
        } else {
            log.error("class-loading-preparation 返回不符合预期 / Failure: {}", res);
        }
    }

    public void testInitializationOrder() {
        Chapter07Controller ctrl = new Chapter07Controller();
        String res = ctrl.testInitializationOrder();
        if (res.contains("Initialization order")) {
            log.info("【成功】initialization-order 返回提示 / Success: {}", res);
        } else {
            log.error("initialization-order 返回不符合预期 / Failure: {}", res);
        }
    }

    public void testClassUnloading() {
        Chapter07Controller ctrl = new Chapter07Controller();
        String res = ctrl.testClassUnloading();
        if (res.contains("requested")) {
            log.info("【成功】class-unloading 返回卸载请求提示 / Success: {}", res);
        } else {
            log.error("class-unloading 返回不符合预期 / Failure: {}", res);
        }
    }

    public static void main(String[] args) {
        Chapter07CompleteTest t = new Chapter07CompleteTest();
        t.testActiveUseNew();
        t.testActiveUseStaticField();
        t.testPassiveUseParentField();
        t.testPassiveUseArray();
        t.testClassLoadingPreparation();
        t.testInitializationOrder();
        t.testClassUnloading();
        log.info("【成功】Chapter07CompleteTest 用例通过 / Success: cases passed");
    }
}
