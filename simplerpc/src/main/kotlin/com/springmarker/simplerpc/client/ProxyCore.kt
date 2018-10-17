package com.springmarker.simplerpc.client

import com.springmarker.simplerpc.annotations.Asyn
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import java.lang.reflect.Method


/**
 * @author Springmarker
 * @date 2018/10/15 21:23
 */
internal class ProxyCore : MethodInterceptor {

    /**
     * 代理类的主要处理方法
     */
    override fun intercept(obj: Any?, method: Method, args: Array<out Any>, proxy: MethodProxy?): Any? {
        val annotations = method.getAnnotation(Asyn::class.java)
        return if (annotations == null) {
            handleSyncRequest(obj, method, args, proxy)
        } else {
            handleAsynRequest(obj, method, args, proxy)
        }
    }

    /**
     * 处理同步方法
     */
    private fun handleSyncRequest(obj: Any?, method: Method, args: Array<out Any>, proxy: MethodProxy?): Any? {
        return null
    }

    /**
     * 处理异步方法
     */
    private fun handleAsynRequest(obj: Any?, method: Method, args: Array<out Any>, proxy: MethodProxy?): Any? {
        return null
    }


}