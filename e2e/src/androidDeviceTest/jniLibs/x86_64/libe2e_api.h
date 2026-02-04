#ifndef KONAN_LIBE2E_H
#define KONAN_LIBE2E_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            libe2e_KBoolean;
#else
typedef _Bool           libe2e_KBoolean;
#endif
typedef unsigned short     libe2e_KChar;
typedef signed char        libe2e_KByte;
typedef short              libe2e_KShort;
typedef int                libe2e_KInt;
typedef long long          libe2e_KLong;
typedef unsigned char      libe2e_KUByte;
typedef unsigned short     libe2e_KUShort;
typedef unsigned int       libe2e_KUInt;
typedef unsigned long long libe2e_KULong;
typedef float              libe2e_KFloat;
typedef double             libe2e_KDouble;
typedef float __attribute__ ((__vector_size__ (16))) libe2e_KVector128;
typedef void*              libe2e_KNativePtr;
struct libe2e_KType;
typedef struct libe2e_KType libe2e_KType;

typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_Byte;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_Short;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_Int;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_Long;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_Float;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_Double;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_Char;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_Boolean;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_Unit;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_UByte;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_UShort;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_UInt;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_ULong;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_ByteArray;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kotlin_CharArray;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kni_test_JvmCaller;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_dev_datlag_nkommons_binding_ByteBuffer;
typedef struct {
  libe2e_KNativePtr pinned;
} libe2e_kref_kni_test__JvmCallerNativeImpl;

extern libe2e_KInt Java_kni_test_CallTestKt_askJvmForANumber(void* env, void* clazz);
extern void* Java_kni_test_CallTestKt_askJvmToFillBuffer(void* env, void* clazz, void* p0);
extern void* Java_kni_test_Bridge_byteArray(void* env, void* clazz, libe2e_KInt p0);
extern void Java_kni_test_CallTestKt_dispose(void* env, void* clazz);
extern void Java_kni_test_CallTestKt_init(void* env, void* clazz, void* p0);
extern void* Java_kni_test_Bridge_mixed(void* env, void* clazz, libe2e_KLong p0, void* p1, libe2e_KUByte p2);
extern void* Java_kni_test_CallTestKt_sendTypeAlias(void* env, void* clazz, void* p0);
extern void* Java_kni_test_Bridge_uppercase(void* env, void* clazz, void* p0);
extern void* Java_kni_test_Bridge_withTypeAlias(void* env, void* clazz, void* p0);

typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(libe2e_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  libe2e_KBoolean (*IsInstance)(libe2e_KNativePtr ref, const libe2e_KType* type);
  libe2e_kref_kotlin_Byte (*createNullableByte)(libe2e_KByte);
  libe2e_KByte (*getNonNullValueOfByte)(libe2e_kref_kotlin_Byte);
  libe2e_kref_kotlin_Short (*createNullableShort)(libe2e_KShort);
  libe2e_KShort (*getNonNullValueOfShort)(libe2e_kref_kotlin_Short);
  libe2e_kref_kotlin_Int (*createNullableInt)(libe2e_KInt);
  libe2e_KInt (*getNonNullValueOfInt)(libe2e_kref_kotlin_Int);
  libe2e_kref_kotlin_Long (*createNullableLong)(libe2e_KLong);
  libe2e_KLong (*getNonNullValueOfLong)(libe2e_kref_kotlin_Long);
  libe2e_kref_kotlin_Float (*createNullableFloat)(libe2e_KFloat);
  libe2e_KFloat (*getNonNullValueOfFloat)(libe2e_kref_kotlin_Float);
  libe2e_kref_kotlin_Double (*createNullableDouble)(libe2e_KDouble);
  libe2e_KDouble (*getNonNullValueOfDouble)(libe2e_kref_kotlin_Double);
  libe2e_kref_kotlin_Char (*createNullableChar)(libe2e_KChar);
  libe2e_KChar (*getNonNullValueOfChar)(libe2e_kref_kotlin_Char);
  libe2e_kref_kotlin_Boolean (*createNullableBoolean)(libe2e_KBoolean);
  libe2e_KBoolean (*getNonNullValueOfBoolean)(libe2e_kref_kotlin_Boolean);
  libe2e_kref_kotlin_Unit (*createNullableUnit)(void);
  libe2e_kref_kotlin_UByte (*createNullableUByte)(libe2e_KUByte);
  libe2e_KUByte (*getNonNullValueOfUByte)(libe2e_kref_kotlin_UByte);
  libe2e_kref_kotlin_UShort (*createNullableUShort)(libe2e_KUShort);
  libe2e_KUShort (*getNonNullValueOfUShort)(libe2e_kref_kotlin_UShort);
  libe2e_kref_kotlin_UInt (*createNullableUInt)(libe2e_KUInt);
  libe2e_KUInt (*getNonNullValueOfUInt)(libe2e_kref_kotlin_UInt);
  libe2e_kref_kotlin_ULong (*createNullableULong)(libe2e_KULong);
  libe2e_KULong (*getNonNullValueOfULong)(libe2e_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      struct {
        struct {
          struct {
            libe2e_KType* (*_type)(void);
            libe2e_kref_kni_test__JvmCallerNativeImpl (*_JvmCallerNativeImpl)(void* env, void* instance);
            const char* (*fillBuffer)(libe2e_kref_kni_test__JvmCallerNativeImpl thiz, libe2e_kref_dev_datlag_nkommons_binding_ByteBuffer buffer);
            libe2e_KInt (*giveANumber)(libe2e_kref_kni_test__JvmCallerNativeImpl thiz);
            const char* (*withTypeAlias)(libe2e_kref_kni_test__JvmCallerNativeImpl thiz, const char* alias);
          } _JvmCallerNativeImpl;
          struct {
            libe2e_KType* (*_type)(void);
            const char* (*fillBuffer)(libe2e_kref_kni_test_JvmCaller thiz, libe2e_kref_dev_datlag_nkommons_binding_ByteBuffer buffer);
            libe2e_KInt (*giveANumber)(libe2e_kref_kni_test_JvmCaller thiz);
            const char* (*withTypeAlias)(libe2e_kref_kni_test_JvmCaller thiz, const char* alias);
          } JvmCaller;
          libe2e_KInt (*_askJvmForANumberJNI)(void* env, void* clazz);
          void* (*_askJvmToFillBufferJNI)(void* env, void* clazz, void* p0);
          void* (*_byteArrayJNI)(void* env, void* clazz, libe2e_KInt p0);
          void (*_disposeJNI)(void* env, void* clazz);
          void (*_initJNI)(void* env, void* clazz, void* p0);
          void* (*_mixedJNI)(void* env, void* clazz, libe2e_KLong p0, void* p1, libe2e_KUByte p2);
          void* (*_sendTypeAliasJNI)(void* env, void* clazz, void* p0);
          void* (*_uppercaseJNI)(void* env, void* clazz, void* p0);
          void* (*_withTypeAliasJNI)(void* env, void* clazz, void* p0);
          libe2e_kref_kotlin_ByteArray (*byteArray)(libe2e_KInt size);
          libe2e_kref_kotlin_ByteArray (*mixed)(libe2e_KLong number, libe2e_kref_kotlin_CharArray value, libe2e_KBoolean upper);
          const char* (*uppercase)(const char* lower);
          const char* (*withTypeAlias)(const char* long_);
          libe2e_kref_kni_test_JvmCaller (*get_callerRef)();
          void (*set_callerRef)(libe2e_kref_kni_test_JvmCaller set);
          libe2e_KInt (*askJvmForANumber)();
          const char* (*askJvmToFillBuffer)(libe2e_kref_dev_datlag_nkommons_binding_ByteBuffer buffer);
          void (*dispose)();
          void (*init)(libe2e_kref_kni_test_JvmCaller caller);
          const char* (*sendTypeAlias)(const char* alias);
        } test;
      } kni;
      const char* (*get_a)();
    } root;
  } kotlin;
} libe2e_ExportedSymbols;
extern libe2e_ExportedSymbols* libe2e_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_LIBE2E_H */
