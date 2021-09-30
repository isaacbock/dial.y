const express = require("express");
const app = express();
const VoiceResponse = require("twilio").twiml.VoiceResponse;
let port = process.env.PORT || 3000;

app.get("/", (req, res) => {
	res.send("Hello world.");
});

app.get("/start", (req, res) => {
	const response = new VoiceResponse();
	response.say(
		"Hi! I'm Siri, an automated tool calling on behalf of a customer with a question."
	);
	response.redirect(
		{
			method: "GET",
		},
		"/askQuestion"
	);

	let twiml = response.toString();
	res.header("Content-Type", "application/xml");
	res.send(twiml);
});

app.get("/askQuestion", (req, res) => {
	// hardcoded, needs to come from API later
	let question = "What are your hours today?";

	const response = new VoiceResponse();
	response.say("They're wondering");
	response.say(question);

	const gather = response.gather({
		action: "/recordAnswer",
		method: "GET",
		numDigits: "1",
		timeout: "6",
		speechTimeout: "5",
		input: "dtmf speech",
		speechModel: "numbers_and_commands",
		hints: "respond, repeat, cancel",
	});
	gather.say(
		"To record a response to this question, say Respond, or press 1. I'll record a voicemail of your answer and transcribe it for the customer. To repeat the question, say Repeat, or press 2. To end the call, say Cancel, or press 3."
	);
	gather.say(
		"To record a response to this question, say Respond, or press 1. I'll record a voicemail of your answer and transcribe it for the customer. To repeat the question, say Repeat, or press 2. To end the call, say Cancel, or press 3."
	);

	response.say("We didn't receive any input. Goodbye!");

	let twiml = response.toString();
	res.header("Content-Type", "application/xml");
	res.send(twiml);
});

app.get("/recordAnswer", (req, res) => {
	if (req.Digits == 1 || req.SpeechResult.toLowerCase().includes("record")) {
		const response = new VoiceResponse();
		response.say("Please record your response after the beep.");
		// change sound to beep
		response.play("https://api.twilio.com/cowbell.mp3");
		response.record({
			action: "/saveRecording",
			timeout: 10,
			transcribe: true,
		});
		let twiml = response.toString();
		res.header("Content-Type", "application/xml");
		res.send(twiml);
	} else if (
		req.Digits == 2 ||
		req.SpeechResult.toLowerCase().includes("repeat")
	) {
		const response = new VoiceResponse();
		response.say("That's okay. Goodbye!");
		let twiml = response.toString();
		res.header("Content-Type", "application/xml");
		res.send(twiml);
	} else {
		const response = new VoiceResponse();
		response.say("Okay! Goodbye!");
		let twiml = response.toString();
		res.header("Content-Type", "application/xml");
		res.send(twiml);
	}
});

app.get("/saveRecording", (req, res) => {
	console.log("Recording URL: " + req.RecordingUrl);
});

app.listen(port, () => {
	console.log(`Listening on port http://localhost:${port}`);
});

// Twilio init
const accountSid = "ACe40dc0c6bc23bc4d12b60b154582ea33";
const authToken = "ba725ef7dddfa4a91a7e01b467b3a180";
const client = require("twilio")(accountSid, authToken);

function initiateCall(phoneNumber) {
	client.calls
		.create({
			url: "/start",
			to: "+1" + phoneNumber,
			from: "+19204208162",
		})
		.then((call) => console.log(call.sid));
}
