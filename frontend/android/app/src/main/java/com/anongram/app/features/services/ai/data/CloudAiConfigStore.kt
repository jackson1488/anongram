package com.anongram.app.features.services.ai.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Локальное шифрованное хранилище профилей OpenAI-совместимых провайдеров
 * и парсер JSON-конфигов в формате OpenCode.
 */
object CloudAiConfigStore {
    private const val PREFS_NAME = "anongram_cloud_ai_cfg"
    private const val KEY_PROFILES = "profiles_json"
    private const val KEY_ALIAS = "anongram_cloud_ai_aes"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val GCM_TAG_BITS = 128

    private fun getOrCreateSecretKey(): SecretKey? {
        return runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = ks.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing
            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }.getOrNull()
    }

    private fun encryptKey(plain: String): String {
        if (plain.isEmpty()) return ""
        val key = getOrCreateSecretKey() ?: return "raw:" + Base64.encodeToString(plain.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val enc = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encB64 = Base64.encodeToString(enc, Base64.NO_WRAP)
            "gcm:$ivB64:$encB64"
        }.getOrElse {
            "raw:" + Base64.encodeToString(plain.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    private fun decryptKey(stored: String): String {
        if (stored.isEmpty()) return ""
        if (stored.startsWith("raw:")) {
            return runCatching {
                String(Base64.decode(stored.removePrefix("raw:"), Base64.NO_WRAP), Charsets.UTF_8)
            }.getOrDefault("")
        }
        if (!stored.startsWith("gcm:")) return stored
        val parts = stored.removePrefix("gcm:").split(":")
        if (parts.size != 2) return ""
        val key = getOrCreateSecretKey() ?: return ""
        return runCatching {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val enc = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(enc), Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun loadProfiles(context: Context): List<CloudProviderProfile> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PROFILES, null)
        val presets = CloudProviderPresets.defaultPresets()
        if (raw.isNullOrBlank()) return presets

        val saved = runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { idx ->
                val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val name = obj.optString("name", id)
                val baseUrl = obj.optString("baseUrl").trimEnd('/')
                val apiKey = decryptKey(obj.optString("apiKeyEnc", ""))
                val isPreset = obj.optBoolean("isPreset", false)
                val modelsArr = obj.optJSONArray("models") ?: JSONArray()
                val models = (0 until modelsArr.length()).mapNotNull { mIdx ->
                    val mObj = modelsArr.optJSONObject(mIdx) ?: return@mapNotNull null
                    val mId = mObj.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    CloudModelInfo(
                        id = mId,
                        displayName = mObj.optString("displayName", mId),
                        supportsVision = mObj.optBoolean("supportsVision", false),
                        supportsTools = mObj.optBoolean("supportsTools", true)
                    )
                }
                CloudProviderProfile(
                    id = id,
                    name = name,
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    models = models,
                    isPreset = isPreset
                )
            }
        }.getOrDefault(emptyList())

        if (saved.isEmpty()) return presets
        val savedIds = saved.map { it.id }.toSet()
        return saved + presets.filter { it.id !in savedIds }
    }

    fun saveProfiles(context: Context, profiles: List<CloudProviderProfile>) {
        val arr = JSONArray()
        profiles.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("baseUrl", p.baseUrl.trimEnd('/'))
            obj.put("apiKeyEnc", encryptKey(p.apiKey))
            obj.put("isPreset", p.isPreset)
            val mArr = JSONArray()
            p.models.forEach { m ->
                val mObj = JSONObject()
                mObj.put("id", m.id)
                mObj.put("displayName", m.displayName)
                mObj.put("supportsVision", m.supportsVision)
                mObj.put("supportsTools", m.supportsTools)
                mArr.put(mObj)
            }
            obj.put("models", mArr)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROFILES, arr.toString())
            .apply()
    }

    /**
     * Парсит JSON-конфиг формата OpenCode:
     * 1) { "provider": { "iishko": { "name": "...", "options": { "baseURL", "apiKey" }, "models": { ... } } } }
     * 2) { "iishko": { "name": "...", "options": { ... }, "models": { ... } } }
     * 3) "iishko": { ... } (фрагмент без внешних фигурных скобок)
     * 4) { "name": "...", "options": { ... }, "models": { ... } }
     */
    fun importOpenCodeJson(rawInput: String): List<CloudProviderProfile> {
        var trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            trimmed = "{$trimmed}"
        }
        // Если пользователь вставил хвост с "shell": ... или лишнюю запятую
        val root = runCatching { JSONObject(trimmed) }.getOrElse {
            val wrapped = "{${trimmed.trimEnd(',')}}"
            JSONObject(wrapped)
        }

        val providerMap: JSONObject = when {
            root.has("provider") && root.optJSONObject("provider") != null -> root.getJSONObject("provider")
            root.has("options") && root.has("models") -> JSONObject().put(
                root.optString("name", "custom").lowercase().replace(Regex("[^a-z0-9_-]"), "_"),
                root
            )
            else -> root
        }

        val result = mutableListOf<CloudProviderProfile>()
        val keys = providerMap.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val pObj = providerMap.optJSONObject(key) ?: continue
            val options = pObj.optJSONObject("options")
            val baseUrl = (options?.optString("baseURL")?.takeIf { it.isNotBlank() }
                ?: options?.optString("baseUrl")?.takeIf { it.isNotBlank() }
                ?: pObj.optString("baseURL").takeIf { it.isNotBlank() }
                ?: pObj.optString("baseUrl")).trimEnd('/')
            if (baseUrl.isBlank()) continue

            val apiKey = options?.optString("apiKey")?.takeIf { it.isNotBlank() }
                ?: pObj.optString("apiKey", "")
            val displayName = pObj.optString("name", key).takeIf { it.isNotBlank() } ?: key

            val modelsObj = pObj.optJSONObject("models")
            val models = mutableListOf<CloudModelInfo>()
            if (modelsObj != null) {
                val mKeys = modelsObj.keys()
                while (mKeys.hasNext()) {
                    val mId = mKeys.next()
                    val mObj = modelsObj.optJSONObject(mId)
                    val mName = mObj?.optString("name", mId)?.takeIf { it.isNotBlank() } ?: mId
                    val modalities = mObj?.optJSONObject("modalities")
                    val inputArr = modalities?.optJSONArray("input")
                    var hasImage = false
                    if (inputArr != null) {
                        for (i in 0 until inputArr.length()) {
                            if (inputArr.optString(i).equals("image", ignoreCase = true)) {
                                hasImage = true
                                break
                            }
                        }
                    }
                    models.add(
                        CloudModelInfo(
                            id = mId,
                            displayName = mName,
                            supportsVision = hasImage,
                            supportsTools = true
                        )
                    )
                }
            }

            result.add(
                CloudProviderProfile(
                    id = key.trim().ifBlank { "custom_${System.currentTimeMillis()}" },
                    name = displayName,
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    models = models,
                    isPreset = false
                )
            )
        }
        return result
    }
}
