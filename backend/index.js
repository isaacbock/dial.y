require("dotenv").config();
const express = require("express");
const VoiceResponse = require("twilio").twiml.VoiceResponse;
const app = express();
app.use(express.json());
app.use(
	express.urlencoded({
		extended: true,
	})
);
let port = process.env.PORT || 3000;

// Twilio init
const accountSid = process.env.TWILIO_ACCOUNT_SID;
const authToken = process.env.TWILIO_AUTH_TOKEN;
const client = require("twilio")(accountSid, authToken);

function initiateCall(phoneNumber) {
	client.calls
		.create({
			url: "https://cse437s-phone.herokuapp.com/start",
			to: "+1" + phoneNumber,
			from: "+15153165732",
		})
		.then((call) => console.log("Call ID: " + call.sid));
}

app.get("/", (req, res) => {
	res.send("Hello world.");
});

app.post("/call", (req, res) => {
	initiateCall(req.body.phoneNumber);
	res.send("Calling " + req.body.phoneNumber);
});

app.post("/start", (req, res) => {
	const response = new VoiceResponse();
	response.pause({ length: 2 });
	response.say("Hi! I'm calling on behalf of a customer with a question.");
	response.redirect({ method: "POST" }, "/askQuestion");

	let twiml = response.toString();
	res.header("Content-Type", "application/xml");
	res.send(twiml);
});

app.post("/askQuestion", (req, res) => {
	// hardcoded, needs to come from API later
	let question = "What are your hours today?";

	const response = new VoiceResponse();
	response.pause({ length: 1 });
	response.say("They're wondering,");
	response.say(question);
	response.pause({ length: 1 });
	response.say(
		"When you're ready, I can record your answer to this question and send it to the customer."
	);
	response.pause({ length: 1 });
	response.redirect({ method: "POST" }, "/promptListener");

	let twiml = response.toString();
	res.header("Content-Type", "application/xml");
	res.send(twiml);
});

app.post("/promptListener", (req, res) => {
	const response = new VoiceResponse();
	const gather = response.gather({
		action: "/recordAnswer",
		method: "POST",
		numDigits: "1",
		timeout: "6",
		speechTimeout: "5",
		input: "speech dtmf",
		speechModel: "numbers_and_commands",
		hints: "respond, repeat, cancel",
	});
	gather.say("To start recording your response, say Respond, or press 1.");
	gather.say("To repeat their question again, say Repeat, or press 2.");
	gather.say(
		"To hang up without recording a response, say Cancel, or press 3."
	);
	gather.pause({ length: 5 });
	gather.say("To start recording your response, say Respond, or press 1.");
	gather.say("To repeat their question again, say Repeat, or press 2.");
	gather.say(
		"To hang up without recording a response, say Cancel, or press 3."
	);
	gather.pause({ length: 5 });

	response.say("Sorry, we didn't receive any input. Goodbye!");

	let twiml = response.toString();
	res.header("Content-Type", "application/xml");
	res.send(twiml);
});

app.post("/recordAnswer", (req, res) => {
	if (req.body.Digits === undefined) {
		req.body.Digits = 0;
	}
	if (req.body.SpeechResult === undefined) {
		req.body.SpeechResult = "";
	}

	if (
		req.body.Digits == "1" ||
		req.body.SpeechResult.toLowerCase().includes("respond")
	) {
		const response = new VoiceResponse();
		response.say(
			"Please record your response after the beep. When you're done recording, hang up, or press 1 to end the call."
		);
		response.pause({ length: 1 });
		response.record({
			action: "/saveRecording",
			timeout: 3,
			transcribe: true,
			transcribeCallback: "/saveTranscription",
		});
		let twiml = response.toString();
		res.header("Content-Type", "application/xml");
		res.send(twiml);
	} else if (
		req.body.Digits == "2" ||
		req.body.SpeechResult.toLowerCase().includes("repeat")
	) {
		const response = new VoiceResponse();
		response.redirect({ method: "POST" }, "/askQuestion");

		let twiml = response.toString();
		res.header("Content-Type", "application/xml");
		res.send(twiml);
	} else if (
		req.body.Digits == "3" ||
		req.body.SpeechResult.toLowerCase().includes("cancel")
	) {
		const response = new VoiceResponse();
		response.say("Okay! Goodbye!");
		let twiml = response.toString();
		res.header("Content-Type", "application/xml");
		res.send(twiml);
	} else {
		const response = new VoiceResponse();
		response.say("Sorry, I didn't understand that.");
		response.redirect({ method: "POST" }, "promptListener");

		let twiml = response.toString();
		res.header("Content-Type", "application/xml");
		res.send(twiml);
	}
});

app.post("/saveRecording", (req, res) => {
	console.log("Recording URL: " + req.body.RecordingUrl);

	const response = new VoiceResponse();
	response.say(
		"Your recording has been saved and sent to the customer. Thank you!"
	);
	let twiml = response.toString();
	res.header("Content-Type", "application/xml");
	res.send(twiml);
});

app.post("/saveTranscription", (req, res) => {
	console.log("Transcription: " + req.body.TranscriptionText);
	res.end();
});

app.listen(port, () => {
	console.log(`Listening on port ${port}.`);
});
