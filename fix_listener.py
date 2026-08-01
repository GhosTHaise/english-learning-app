with open('app/src/main/java/com/example/ui/screens/TutorScreen.kt', 'r') as f:
    content = f.read()

# Replace the broken listener block
broken_listener = """    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
        override fun onBeginningOfSpeech() {
        override fun onRmsChanged(rmsdB: Float) {
            onRmsChange(rmsdB)
        }
        override fun onBufferReceived(buffer: ByteArray?) {
        override fun onEndOfSpeech() {
        override fun onError(error: Int) {
        override fun onResults(results: Bundle?) {
            setListening(false)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onResult(matches[0])
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {
        override fun onEvent(eventType: Int, params: Bundle?) {
    })"""

fixed_listener = """    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {
            onRmsChange(rmsdB)
        }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            setListening(false)
        }
        override fun onResults(results: Bundle?) {
            setListening(false)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onResult(matches[0])
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })"""

content = content.replace(broken_listener, fixed_listener)

with open('app/src/main/java/com/example/ui/screens/TutorScreen.kt', 'w') as f:
    f.write(content)
