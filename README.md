# Ktorfit

Ktorfit 是一个 Kotlin Multiplatform 的网络接口框架，面向 Kotlin Multiplatform 开发者，支持 Android / iOS / JVM(Desktop)。
它提供类似 Retrofit 的注解风格(Android遗老的福音)，通过 KSP 在编译期生成实现，运行时通过 Ktor 发起请求。

## 特性

- Android / iOS / JVM(Desktop)
- 通过注解定义接口，KSP 生成实现
- 运行时基于 Ktor，可自由配置 HttpClient
- 支持 GET/POST/PUT/DELETE/PATCH/HEAD/OPTIONS/HTTP
- 支持 Headers/Query/Path/Body/Field/Part 等常用参数

## 安装

### 1) 根工程添加仓库（JitPack）

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
```

### 2) 在使用模块中添加依赖

```kotlin
dependencies {
    // 运行时 + 注解
    implementation("com.github.fengpeitian.Ktorfit:ktorfit:1.0.0")

    // KSP 处理器(根据情况选择)
    add("kspCommonMainMetadata", "com.github.fengpeitian.Ktorfit:ktorfit-ksp:1.0.0")
    add("kspAndroid", "com.github.fengpeitian.Ktorfit:ktorfit-ksp:1.0.0")
    add("kspJvm", "com.github.fengpeitian.Ktorfit:ktorfit-ksp:1.0.0")
    add("kspIosArm64", "com.github.fengpeitian.Ktorfit:ktorfit-ksp:1.0.0")
    add("kspIosSimulatorArm64", "com.github.fengpeitian.Ktorfit:ktorfit-ksp:1.0.0")
}
```

### 3) 启用 KSP 插件

```kotlin
plugins {
    id("com.google.devtools.ksp")
}
```

### 4) 配置 KSP 生成目录（KMP 必需，根据情况选择）

```kotlin
kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }
        jvmMain {
            kotlin.srcDir("build/generated/ksp/jvm/jvmMain/kotlin")
        }
        iosArm64Main {
            kotlin.srcDir("build/generated/ksp/iosArm64/iosArm64Main/kotlin")
        }
        iosSimulatorArm64Main {
            kotlin.srcDir("build/generated/ksp/iosSimulatorArm64/iosSimulatorArm64Main/kotlin")
        }
    }
}
```

## 使用方式（示例）

### 1) 定义接口（commonMain）

```kotlin
import io.github.fpt.ktorfit.annotations.Body
import io.github.fpt.ktorfit.annotations.GET
import io.github.fpt.ktorfit.annotations.POST
import io.github.fpt.ktorfit.annotations.Path
import io.github.fpt.ktorfit.annotations.Query

data class User(val id: Long, val name: String)
data class CreateUser(val name: String)
data class ApiResp<T>(val code: Int, val message: String, val data: T?)

interface UserApi {
    @GET("/api/users/{id}")
    suspend fun getUser(
        @Path("id") id: Long,
        @Query("detail") detail: Boolean = false
    ): ApiResp<User?>

    @POST("/api/users")
    suspend fun createUser(@Body body: CreateUser): ApiResp<User?>
}
```

### 2) 创建 Ktorfit 并调用

```kotlin
val httpClient: KtorfitHttpClient = /* 你自己的实现，示例见 sample */
val ktorfit = Ktorfit(
    baseUrl = "https://example.com",
    httpClient = httpClient
)

val api = ktorfit.create(UserApi::class)
val user = api.getUser(id = 1).data
```

## 注解列表

### 请求方法

- `@GET(path)`
- `@POST(path)`
- `@PUT(path)`
- `@DELETE(path)`
- `@PATCH(path)`
- `@HEAD(path)`
- `@OPTIONS(path)`
- `@HTTP(method, path = "", hasBody = false)`

### 传参与请求配置

- `@Headers(vararg value: String)`
- `@FormUrlEncoded`
- `@Multipart`
- `@Streaming`

### 参数注解

- `@Body`
- `@Path(name = "")`
- `@Query(name = "")`
- `@QueryMap`
- `@Field(name = "")`
- `@FieldMap`
- `@Part(name = "")`
- `@PartMap`
- `@Header(name = "")`
- `@Url`

## 原理简述

### 1) 编译期：KSP 生成实现

接口中带 HTTP 注解的方法会在编译期由 KSP 生成 `XXXImpl` 实现类，并注册到 `KtorfitRegistry`。  
iOS 端使用 `@KtorfitEagerInit` 保证注册代码不会被裁剪。

### 2) 运行时：创建与调用

- Android/JVM：反射 `XXXImpl`，或者从注册表获取
- iOS：仅从注册表获取
- 请求通过 `KtorfitHttpClient` 发起，最终由你传入的 Ktor `HttpClient` 完成网络调用与解析

## 目录结构

- `ktorfit`：运行时 + 注解定义（KMP）
- `ktorfit-ksp`：KSP 处理器，生成实现类
- `ktorfit-sample`：示例工程

## 版本与平台

- 平台：Android / iOS / JVM(Desktop)
- 版本：`1.0.0`
