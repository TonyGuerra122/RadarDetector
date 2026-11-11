package com.tonyguerra.radardetector.data.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkAccessTest {

    private fun local(url: String) = LocalNetworkAccess.isLocalNetworkUrl(url)

    @Test
    fun `enderecos de rede local exigem permissao`() {
        assertTrue(local("http://10.0.2.2:8080"))
        assertTrue(local("http://192.168.0.15:8080"))
        assertTrue(local("http://172.16.4.1"))
        assertTrue(local("http://172.31.255.255/api"))
        assertTrue(local("http://169.254.10.20"))
        assertTrue(local("http://meu-pc.local:8080"))
        assertTrue(local("http://[fd12:3456::1]:8080"))
        assertTrue(local("http://[fe80::1]"))
    }

    @Test
    fun `internet publica e loopback nao exigem permissao`() {
        assertFalse(local("https://api.meudominio.com"))
        assertFalse(local("https://8.8.8.8"))
        assertFalse(local("http://172.32.0.1"))
        assertFalse(local("http://172.15.0.1"))
        assertFalse(local("http://localhost:8080"))
        assertFalse(local("http://127.0.0.1:8080"))
        assertFalse(local("http://[::1]:8080"))
        assertFalse(local("http://10.0.2.2.exemplo.com"))
        assertFalse(local("isso nao e url"))
    }
}
