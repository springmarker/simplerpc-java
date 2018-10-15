package com.springmarker.simplerpc.client

import net.sf.cglib.proxy.Enhancer
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Springmarker
 * @date 2018/10/15 21:12
 */
class RpcClientFactory {

    private val nameMap: ConcurrentHashMap<String, Any?> = ConcurrentHashMap()

    private val proxyCore: ProxyCore = ProxyCore()

    /**
     * 根据clazz获取代理对象。
     *
     * @param clazz 某个接口的 [Class]
     * @return clazz实体代理对象
     */
    fun <T> get(clazz: Class<T>): T? {
        val any = nameMap[clazz.name] ?: return null
        return any as T
    }

    /**
     * 根据 clazz 创建代理类，并添加到 [RpcClientFactory] 中，clazz 必须为一个接口类型。
     *
     * @param clazz 某个接口的 [Class]
     * @return 返回值为[Boolean] 类型，为true时表示创建成功，false表示创建失败。
     */
    fun add(clazz: Class<*>): Boolean {
        if (!clazz.isInterface) {
            return false
        }
        val proxy = creatProxy(clazz)
        nameMap[clazz.canonicalName] = proxy
        return true
    }


    private fun creatProxy(clazz: Class<*>): Any {
        val enhancer = Enhancer()
        enhancer.setSuperclass(clazz)
        enhancer.setCallback(proxyCore)
        return enhancer.create()
    }

}