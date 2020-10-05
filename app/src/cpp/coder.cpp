#include <android/log.h>
#include <bitset>
#include <iostream>
#include <jni.h>
#include <stdint.h>
#include <sstream>
#include <string>
#include <vector>
#include "ezpwd/rs"

#define  LOG_TAG     "cppCoder"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

#define CODEWIDTH 24
#define MAXERRORS_ONEBIT 8
#define CH3_NEAR 0


unsigned short bitsetUshortFromRange(std::bitset<8> bs, size_t start, size_t end) {
    // Returns the range of bits in the bitset as an unsigned short.
    // Indices from least to most significant bit.

    /* DEBUG
    std::cout << bs.to_string() << " " << start << " " << end << std::endl;
    //*/

    unsigned short mask = 1;
    unsigned short result = 0;
    for (size_t i = start; i < end; ++i) {
        if (bs.test(i)) {
            result |= mask;
        }
        mask <<= 1;
    }

    /* DEBUG
    std::cout << result << std::endl;
    //*/

    return result;
}

extern "C" {

JNIEXPORT jintArray JNICALL
Java_fi_aalto_roopepalomaki_distanceawarebarcode_BarcodeOneBit_cppEncodeRS255onebit(
JNIEnv *env, jclass cls, jstring jstr) {
    // Returns the input split into 1-bit integers.
    // Prepends size (1 byte) and appends Reed-Solomon parity bits.
    //
    // Uses RS<255, k> internally but shortens to RS<code_capacity, k>.le

    const char *chars = env->GetStringUTFChars(jstr, NULL);
    std::string data_str(chars);
    std::vector<uint8_t> data(data_str.begin(), data_str.end()); // ignore buggy android studio compiler warning

    const int code_capacity = CODEWIDTH * CODEWIDTH / 8;
    const int size_of_size = 1;
    const int rs_n = 255; // RS code word size
    const int rs_t = MAXERRORS_ONEBIT; // RS max errors
    const int max_data_size = code_capacity - 2 * rs_t;
    const int rs_k = rs_n - 2 * rs_t; // RS data size

    const int size = std::min(max_data_size - size_of_size, static_cast<int>(data.size()));
    data.insert(data.begin(), static_cast<uint8_t>(size));

    const int data_size = static_cast<int>(data.size()); // size of (size byte + input payload even if too large)
    const int padding_size = max_data_size - data_size;

    // explicit 0 padding for data sizes different than max data size (transmitted)
    if (padding_size != 0) {
        data.resize(static_cast<unsigned int>(data_size + padding_size), 0);
    }

    // implicit 0 padding to shorten RS<255, k> to RS<code_capacity, k> (not transmitted)
    data.resize(static_cast<unsigned long>(rs_k), 0);

    ezpwd::RS<rs_n, rs_k> rs;
    std::vector<uint8_t> parity;
    rs.encode(data, parity);

    // remove implicit padding
    data.resize(static_cast<unsigned long>(max_data_size));

    //* DEBUG
    LOGD("encode - data size: %d", static_cast<int>(data.size()));
    LOGD("encode - parity size: %d", static_cast<int>(parity.size()));
    LOGD("encode - data: %s", std::string(data.begin(), data.end()).c_str());
    LOGD("encode - parity: %s", std::string(parity.begin(), parity.end()).c_str());
    //*/

    //* 1-BIT INTS
    std::vector<unsigned short> output;

    for (size_t i = 0; i < data.size(); ++i) {
        std::bitset<8> bs = std::bitset<8>(data[i]);
        for (size_t j = 8; j > 0; --j) {
            output.push_back(bitsetUshortFromRange(bs, (j-1), j));
        }
    }

    for (size_t i = 0; i < parity.size(); ++i) {
        std::bitset<8> bs = std::bitset<8>(parity[i]);
        for (size_t j = 8; j > 0; --j) {
            output.push_back(bitsetUshortFromRange(bs, (j-1), j));
        }
    }

    //* DEBUG
    LOGD("encode - number of bits: %d", static_cast<int>(output.size()));
    //*/

    // copy to JNI format int array
    const int symbols_per_byte = 8;
    jintArray joutput = env->NewIntArray(code_capacity * symbols_per_byte);
    jint *jout = env->GetIntArrayElements(joutput, 0);
    for (int i = 0; i < code_capacity * symbols_per_byte; ++i) {
        jout[i] = static_cast<int>(output[i]);
    }
    env->ReleaseIntArrayElements(joutput, jout, 0);

    return joutput;
}

JNIEXPORT jbyteArray JNICALL
Java_fi_aalto_roopepalomaki_distanceawarebarcode_CVOneBit_cppDecodeRS255onebit(
JNIEnv *env, jobject, jintArray jints) {
    // Returns the string encoded in the data.
    // Tries to repair errors with Reed-Solomon.
    //
    // Assumes encoding was done with RS<255, k> shortened
    // to RS<code_capacity, k>.
    //
    // Expects a vector of individual bits.

    // copy from JNI to C++ vector (unnecessary copy but keeps C++ intact)
    std::vector<uint8_t> ints; // received bits as ints
    jsize len = env->GetArrayLength(jints);
    jint *ji = env->GetIntArrayElements(jints, 0);
    for (size_t i = 0; i < len; ++i) {
        ints.push_back(static_cast<uint8_t>(ji[i]));
    }
    env->ReleaseIntArrayElements(jints, ji, 0);

    const int code_capacity = CODEWIDTH * CODEWIDTH / 8;
    const int rs_n = 255; // RS code word size
    const int rs_t = MAXERRORS_ONEBIT; // RS max errors
    const size_t max_data_size = static_cast<size_t>(code_capacity - 2 * rs_t); // shortened data size
    const size_t parity_size = 2 * rs_t;
    const size_t rs_k = rs_n - 2 * rs_t; // RS data size

    std::vector<uint8_t> data;
    std::vector<uint8_t> parity;

    //* ASSUMES INDIVIDUAL BITS AS INPUT
    for (size_t i = 0; i < max_data_size; ++i) {
        size_t j = i * 8;
        uint8_t combined =
        ints[j] << 7 |
        ints[j + 1] << 6 |
        ints[j + 2] << 5 |
        ints[j + 3] << 4 |
        ints[j + 4] << 3 |
        ints[j + 5] << 2 |
        ints[j + 6] << 1 |
        ints[j + 7];
        data.push_back(combined);
    }

    // add implicit padding used to shorten RS<255, k> to RS<code_capacity, k>
    data.resize(rs_k, 0);

    for (size_t i = max_data_size; i < max_data_size + parity_size; ++i) {
        size_t j = i * 8;
        uint8_t combined =
        ints[j] << 7 |
        ints[j + 1] << 6 |
        ints[j + 2] << 5 |
        ints[j + 3] << 4 |
        ints[j + 4] << 3 |
        ints[j + 5] << 2 |
        ints[j + 6] << 1 |
        ints[j + 7];
        parity.push_back(combined);
    }

    //* DEBUG
    LOGD("decode - data size: %d", (int)data.size());
    LOGD("decode - parity size: %d", (int)parity.size());
    LOGD("decode - data string: %s", std::string(data.begin(), data.end()).c_str());
    LOGD("decode - parity string: %s", std::string(parity.begin(), parity.end()).c_str());
    //*/

    ezpwd::RS<rs_n, rs_k> rs;

    int correct = rs.decode(data, parity);
    if (correct >= 0) {
        LOGD("decode - recovered with %d errors", correct);
    } else {
        LOGD("decode - failed to recover data");
    }

    const size_t payload_size = std::min(max_data_size, static_cast<size_t>(data.at(0)));
    data.erase(data.begin()); // remove size byte
    data.resize(payload_size); // clip to payload size

    //* DEBUG
    LOGD("decode - payload size: %d", static_cast<int>(payload_size));
    std::string output(data.begin(), data.end());
    LOGD("decode - output: %s", output.c_str());
    //*/

    // NOTE: does not work with erroneous UTF-8, can't use
    //return env->NewStringUTF(output.c_str());

    // header with success flag and number of errors corrected
    // note: data is uint8_t and number of errors corrected may be -1 (signed)
    data.insert(data.begin(), static_cast<uint8_t>(correct == -1 ? 0 : correct));
    data.insert(data.begin(), static_cast<uint8_t>(correct < 0));

    LOGD("decode - success %s errors %s", (new std::bitset<8>(data[0]))->to_string().c_str(), (new std::bitset<8>(data[1]))->to_string().c_str());

    // C++ bytes to JNI byte array
    jsize jsz = static_cast<jsize>(data.size());
    jbyteArray jarr = env->NewByteArray(jsz);
    env->SetByteArrayRegion(jarr, 0, jsz, reinterpret_cast<jbyte *>(data.data()));

    return jarr;
}

}