package cn.favouritesc.cookingshop.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "NsdHelper"
private const val SERVICE_TYPE = "_cookingshop._tcp."
private const val SERVICE_NAME = "CookingShop"

data class DiscoveredHost(
    val name: String,
    val ip: String,
    val port: Int
)

class NsdHelper(private val context: Context) {

    private val nsdManager: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _discoveredHosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val discoveredHosts: StateFlow<List<DiscoveredHost>> = _discoveredHosts.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    // ===== 服务注册（主机端） =====

    private var registrationListener: NsdManager.RegistrationListener? = null

    fun registerService(port: Int, serviceName: String = SERVICE_NAME) {
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = SERVICE_TYPE
            this.port = port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service registered: ${nsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service registration failed: errorCode=$errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: ${serviceInfo.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service unregistration failed: errorCode=$errorCode")
            }
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        Log.d(TAG, "registerService called: $serviceName on port $port")
    }

    fun unregisterService() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister service", e)
            }
        }
        registrationListener = null
    }

    // ===== 服务发现（客户端） =====

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val resolvedHosts = mutableMapOf<String, DiscoveredHost>()

    fun startDiscovery() {
        stopDiscovery()
        resolvedHosts.clear()
        _discoveredHosts.value = emptyList()
        _isDiscovering.value = true

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Discovery started: $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceName}")
                // 解析服务获取 IP 和端口
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                        val host = resolvedInfo.host ?: return
                        val ip = host.hostAddress ?: return
                        val port = resolvedInfo.port
                        val name = resolvedInfo.serviceName

                        val discovered = DiscoveredHost(name, ip, port)
                        resolvedHosts[name] = discovered
                        _discoveredHosts.value = resolvedHosts.values.toList()
                        Log.d(TAG, "Service resolved: $name @ $ip:$port")
                    }

                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.w(TAG, "Resolve failed: ${serviceInfo.serviceName}, error=$errorCode")
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                resolvedHosts.remove(serviceInfo.serviceName)
                _discoveredHosts.value = resolvedHosts.values.toList()
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed: $serviceType, error=$errorCode")
                _isDiscovering.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: $serviceType, error=$errorCode")
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        Log.d(TAG, "startDiscovery called")
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop discovery", e)
            }
        }
        discoveryListener = null
        _isDiscovering.value = false
        Log.d(TAG, "stopDiscovery called")
    }
}
