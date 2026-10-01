# Gemma phone test

This is a single screen, offline Android test: text input → Gemma → text response. It has no `INTERNET` permission or app download code. The first model load happens when you tap **Analyze**; later taps reuse the loaded engine. Logcat tag: `GemmaPhoneTest`.

## Model choice

Use `gemma3-1b-it-int4.litertlm` from [LiteRT Community's Gemma3-1B-IT repository](https://huggingface.co/litert-community/Gemma3-1B-IT/tree/main). It is 584,417,280 bytes, uses LiteRT-LM, and Google's [AI Edge Gallery model list](https://github.com/google-ai-edge/gallery/blob/main/model_allowlists/1_0_16.json) lists 6 GB minimum device RAM. The larger Gemma 3n E2B model is listed with an 8 GB minimum, so it is a poor first choice until the phone's RAM is confirmed.

The model repository requires you to sign in to Hugging Face and accept the Gemma terms before downloading. The model is deliberately excluded from this source project and APK.

## Run on the phone

1. Enable USB debugging and connect the Xiaomi phone. Open this folder in Android Studio and install the `app` debug configuration. Android Studio may need to download Gradle, Kotlin, and LiteRT-LM dependencies once on the development computer.
2. Download **exactly** `gemma3-1b-it-int4.litertlm` from the linked repository after accepting the license. Put it on the development computer. On macOS or Linux, use these commands, replacing the first path:

   ```sh
   MODEL=/absolute/path/to/gemma3-1b-it-int4.litertlm
   adb devices -l
   adb push "$MODEL" /data/local/tmp/gemma3-1b-it-int4.litertlm
   adb shell run-as com.example.gemmaphonetest cp /data/local/tmp/gemma3-1b-it-int4.litertlm files/gemma3-1b-it-int4.litertlm
   adb shell run-as com.example.gemmaphonetest ls -l files/gemma3-1b-it-int4.litertlm
   ```

   Launch the app at least once before `run-as` so its `files/` directory exists. The app is a debug build, which allows `run-as`. The APK does not need storage permission.
3. Launch the app and tap **Analyze**. The provided login issue prompt is prefilled. The output area displays Gemma's generated text, which should be a category and summary. Exact wording may vary.
4. Watch timing and errors:

   ```sh
   adb logcat -s GemmaPhoneTest:I '*:S'
   ```

   Look for `Model loaded successfully`, `load time=... ms`, and `Inference time=... ms`. An error also appears in the output area and Logcat.

The runtime is [Google LiteRT-LM for Android](https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md), using its CPU backend to avoid depending on Snapdragon GPU or NPU driver support for this first test. If the 1B model fails due to memory pressure, the smallest model to try next is [Gemma 3 270M instruction tuned, 304 MB](https://huggingface.co/litert-community/gemma-3-270m-it/tree/main); change `MODEL_FILE` in `MainActivity.kt` to `gemma3-270m-it-q8.litertlm` and copy that file instead. Its response quality may be lower.
