require("dotenv").config();

const express = require("express");
const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.json());
let port = process.env.PORT || 3000;

const VoiceResponse = require("twilio").twiml.VoiceResponse;
const accountSid = process.env.TWILIO_ACCOUNT_SID;
const authToken = process.env.TWILIO_AUTH_TOKEN;
const client = require("twilio")(accountSid, authToken);

const admin = require("firebase-admin");
const serviceAccount = JSON.parse(process.env.GOOGLE_APPLICATION_CREDENTIALS);
admin.initializeApp({
	credential: admin.credential.cert(serviceAccount),
});
let db = admin.firestore();

app.listen(port, () => {
	console.log(`Listening on port ${port}.`);
});

app.get("/", (req, res) => {
	res.send("Hello world.");
});

app.post("/call", async (req, res) => {
	let toPhoneNumber = "+1" + req.body.phoneNumber;
	let questions = req.body.questions;

	callSID = await client.calls
		.create({
			url: "https://cse437s-phone.herokuapp.com/start",
			to: toPhoneNumber,
			from: "+15153165732",
		})
		.then((call) => {
			console.log("Call " + call.sid + " initiated.");
			return call.sid;
		});

	let callQuestions = [];
	for (question of questions) {
		let newQuestion = {
			question: question,
			status: "Waiting",
			answerAudio: null,
			answerTranscript: null,
		};
		callQuestions.push(newQuestion);
	}
	const call = {
		to: toPhoneNumber,
		status: "Dialing",
		date: new Date(),
		questions: callQuestions,
	};
	db.collection("calls")
		.doc(callSID)
		.set(call)
		.then(() => {
			console.log("Call " + callSID + " added to database.");
			res.send(callSID);
		});
});

app.post("/start", (req, res) => {
	const callSID = req.body.CallSid;

	db.collection("calls")
		.doc(callSID)
		.update({ status: "In Progress" })
		.then(() => {
			console.log("Call " + callSID + " in progress.");

			const response = new VoiceResponse();
			response.pause({ length: 2 });
			response.say("Hi! I'm calling on behalf of a customer with a question.");
			response.redirect({ method: "POST" }, "/askQuestion");

			let twiml = response.toString();
			res.header("Content-Type", "application/xml");
			res.send(twiml);
		});
});

app.post("/askQuestion", async (req, res) => {
	const callSID = req.body.CallSid;

	const callRef = db.collection("calls").doc(callSID);
	const call = await callRef.get();
	if (!call.exists) {
		console.log("Call " + callSid + " not found in database.");
	} else {
		const question = call.data().questions[0].question;

		let questionsUpdate = call.data().questions;
		questionsUpdate[0].status = "Asking";
		callRef.update({ questions: questionsUpdate }).then(() => {
			console.log("Call " + callSid + "-- Asking: " + question);

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
	}
});

app.post("/promptListener", async (req, res) => {
	const callSID = req.body.CallSid;

	const callRef = db.collection("calls").doc(callSID);
	const call = await callRef.get();
	if (!call.exists) {
		console.log("Call " + callSid + " not found in database.");
	} else {
		let questionsUpdate = call.data().questions;
		questionsUpdate[0].status = "Prompting";
		callRef.update({ questions: questionsUpdate }).then(() => {
			console.log("Call " + callSid + "-- Prompting: " + question);
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
			gather.pause({ length: 10 });

			gather.say("To start recording your response, say Respond, or press 1.");
			gather.say("To repeat their question again, say Repeat, or press 2.");
			gather.say(
				"To hang up without recording a response, say Cancel, or press 3."
			);
			gather.pause({ length: 10 });

			response.say("Sorry, we didn't receive any input. Goodbye!");

			let twiml = response.toString();
			res.header("Content-Type", "application/xml");
			res.send(twiml);
		});
	}
});

app.post("/recordAnswer", async (req, res) => {
	const callSID = req.body.CallSid;

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
		const callRef = db.collection("calls").doc(callSID);
		const call = await callRef.get();
		if (!call.exists) {
			console.log("Call " + callSid + " not found in database.");
		} else {
			let questionsUpdate = call.data().questions;
			questionsUpdate[0].status = "Recording";
			callRef.update({ questions: questionsUpdate }).then(() => {
				console.log("Call " + callSid + "-- Recording answer.");

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
			});
		}
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
		const callRef = db.collection("calls").doc(callSID);
		const call = await callRef.get();
		if (!call.exists) {
			console.log("Call " + callSid + " not found in database.");
		} else {
			callRef.update({ status: "Hung Up" }).then(() => {
				console.log("Call " + callSid + "-- Hung up by listener.");

				const response = new VoiceResponse();
				response.say("Okay! Goodbye!");
				let twiml = response.toString();
				res.header("Content-Type", "application/xml");
				res.send(twiml);
			});
		}
	} else {
		const response = new VoiceResponse();
		response.say("Sorry, I didn't understand that.");
		response.redirect({ method: "POST" }, "promptListener");

		let twiml = response.toString();
		res.header("Content-Type", "application/xml");
		res.send(twiml);
	}
});

app.post("/saveRecording", async (req, res) => {
	const callSID = req.body.CallSid;

	const callRef = db.collection("calls").doc(callSID);
	const call = await callRef.get();
	if (!call.exists) {
		console.log("Call " + callSid + " not found in database.");
	} else {
		let questionsUpdate = call.data().questions;
		questionsUpdate[0].status = "Transcribing";
		questionsUpdate[0].answerAudio = req.body.RecordingUrl;
		callRef.update({ questions: questionsUpdate }).then(() => {
			console.log("Call " + callSid + "-- Recording: " + req.body.RecordingUrl);

			const response = new VoiceResponse();
			response.say(
				"Your recording has been saved and sent to the customer. Thank you!"
			);
			let twiml = response.toString();
			res.header("Content-Type", "application/xml");
			res.send(twiml);
		});
	}
});

app.post("/saveTranscription", async (req, res) => {
	const callSID = req.body.CallSid;

	const callRef = db.collection("calls").doc(callSID);
	const call = await callRef.get();
	if (!call.exists) {
		console.log("Call " + callSid + " not found in database.");
	} else {
		let questionsUpdate = call.data().questions;
		questionsUpdate[0].status = "Completed";
		questionsUpdate[0].answerTranscript = req.body.TranscriptionText;
		callRef
			.update({
				status: "Completed",
				questions: questionsUpdate,
			})
			.then(() => {
				console.log(
					"Call " +
						callSid +
						"-- Answer transcribed: " +
						req.body.TranscriptionText
				);

				res.end();
			});
	}
});
