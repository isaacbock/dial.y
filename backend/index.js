require("dotenv").config({ path: __dirname + `/../.env` });

// Imports the Google Cloud client library
const { Translate } = require("@google-cloud/translate").v2;

// Creates a client
const translate = new Translate();

const express = require("express");
const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

let port = process.env.PORT || 3000;

//Imports the Socket.io library
const http = require("http");
const server = http.createServer(app);
const { Server } = require("socket.io");
const io = new Server(server);
server.listen(port, () => {
	console.log(`Listening on port ${port}.`);
});

const VoiceResponse = require("twilio").twiml.VoiceResponse;
const accountSid = process.env.TWILIO_ACCOUNT_SID;
const authToken = process.env.TWILIO_AUTH_TOKEN;
const client = require("twilio")(accountSid, authToken);

const admin = require("firebase-admin");
const serviceAccount = JSON.parse(
	process.env.GOOGLE_APPLICATION_CREDENTIALS_FIREBASE
);
admin.initializeApp({
	credential: admin.credential.cert(serviceAccount),
});
let db = admin.firestore();

const fs = require("fs");
const fetch = require("node-fetch");
const speech = require("@google-cloud/speech");
// save google speech credentials to file with correct name
try {
	fs.writeFileSync(
		process.env.GOOGLE_APPLICATION_CREDENTIALS,
		process.env.GOOGLE_APPLICATION_CREDENTIALS_SPEECH
	);
} catch (err) {
	console.log("Error initializing google speech credentials: " + err);
}

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
		console.log("Call " + callSID + " not found in database.");
	} else {
		const question = call.data().questions[0].question;

		let questionsUpdate = call.data().questions;
		questionsUpdate[0].status = "Asking";
		callRef.update({ questions: questionsUpdate }).then(() => {
			console.log("Call " + callSID + "-- Asking: " + question);

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
		console.log("Call " + callSID + " not found in database.");
	} else {
		let questionsUpdate = call.data().questions;
		questionsUpdate[0].status = "Prompting";
		callRef.update({ questions: questionsUpdate }).then(() => {
			console.log(
				"Call " + callSID + "-- Prompting: " + questionsUpdate[0].question
			);
			const response = new VoiceResponse();
			const gather = response.gather({
				action: "/recordAnswer",
				method: "POST",
				numDigits: "1",
				timeout: "6",
				input: "dtmf",
			});
			gather.say("To start recording your response, press 1.");
			gather.say("To repeat their question again, press 2.");
			gather.say("To hang up without recording a response, press 3.");
			gather.pause({ length: 5 });

			gather.say("To start recording your response, press 1.");
			gather.say("To repeat their question again, press 2.");
			gather.say("To hang up without recording a response, press 3.");
			gather.pause({ length: 10 });

			gather.say("To start recording your response, press 1.");
			gather.say("To repeat their question again, press 2.");
			gather.say("To hang up without recording a response, press 3.");
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

	if (req.body.Digits == "1") {
		const callRef = db.collection("calls").doc(callSID);
		const call = await callRef.get();
		if (!call.exists) {
			console.log("Call " + callSID + " not found in database.");
		} else {
			let questionsUpdate = call.data().questions;
			questionsUpdate[0].status = "Recording";
			callRef.update({ questions: questionsUpdate }).then(() => {
				console.log("Call " + callSID + "-- Recording answer.");

				const response = new VoiceResponse();
				response.say(
					"Please record your response after the beep. When you're done recording, hang up, or press 1 to end the call."
				);
				response.pause({ length: 1 });
				response.record({
					action: "/saveRecording",
					timeout: 3,
				});
				let twiml = response.toString();
				res.header("Content-Type", "application/xml");
				res.send(twiml);
			});
		}
	} else if (req.body.Digits == "2") {
		const response = new VoiceResponse();
		response.redirect({ method: "POST" }, "/askQuestion");

		let twiml = response.toString();
		res.header("Content-Type", "application/xml");
		res.send(twiml);
	} else if (req.body.Digits == "3") {
		const callRef = db.collection("calls").doc(callSID);
		const call = await callRef.get();
		if (!call.exists) {
			console.log("Call " + callSID + " not found in database.");
		} else {
			callRef.update({ status: "Hung Up" }).then(() => {
				console.log("Call " + callSID + "-- Hung up by listener.");

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
	let call = await callRef.get();
	if (!call.exists) {
		console.log("Call " + callSID + " not found in database.");
		res.end();
	} else {
		let questionsUpdate = call.data().questions;
		questionsUpdate[0].status = "Transcribing";
		questionsUpdate[0].answerAudio = req.body.RecordingUrl;
		callRef.update({ questions: questionsUpdate }).then(() => {
			console.log(
				"Call " + callSID + "-- Recording URL: " + req.body.RecordingUrl
			);

			const response = new VoiceResponse();
			response.say(
				"Your recording has been saved and sent to the customer. Thank you!"
			);
			let twiml = response.toString();
			res.header("Content-Type", "application/xml");
			res.send(twiml);

			// Transcribe audio recording
			const url = req.body.RecordingUrl;
			const path = toString(callSID) + ".wav";

			// download audio recording (wait small delay first to ensure that it is available on Twilio)
			let delay = 1000;
			setTimeout(function () {
				fetch(url)
					.then((res) => res.buffer())
					.then((buffer) => {
						fs.writeFileSync(path, buffer);
						let stats = fs.statSync(path);
						let fileSizeInBytes = stats["size"];
						console.log(
							"Audio file downloaded for transcription: " +
								fileSizeInBytes +
								" bytes"
						);
						// Transcribe audio recording
						async function transcribe() {
							// Creates a client
							const client = new speech.SpeechClient();

							const encoding = "LINEAR16";
							const sampleRateHertz = 8000;
							const languageCode = "en-US";

							const config = {
								encoding: encoding,
								languageCode: languageCode,
								sampleRateHertz: sampleRateHertz,
								enableAutomaticPunctuation: true,
								useEnhanced: true,
								model: "phone_call",
							};

							const audio = {
								content: fs.readFileSync(path).toString("base64"),
							};

							const request = {
								config: config,
								audio: audio,
							};

							// Detects speech in the audio file
							const [response] = await client.recognize(request);
							const transcription = response.results
								.map((result) => result.alternatives[0].transcript)
								.join("\n");

							// Update call in database to include transcription
							call = await callRef.get();
							if (!call.exists) {
								console.log("Call " + callSID + " not found in database.");
							} else {
								let questionsUpdate = call.data().questions;
								questionsUpdate[0].status = "Completed";

								// test google translate api
								const text = transcription;
								const target = "ko";

								async function translateText() {
									// Translates the text into the target language. "text" can be a string for
									// translating a single piece of text, or an array of strings for translating
									// multiple texts.
									let [translations] = await translate.translate(text, target);
									translations = Array.isArray(translations)
										? translations
										: [translations];
									console.log("Translations:");
									translations.forEach((translation, i) => {
										console.log(`${text[i]} => (${target}) ${translation}`);
									});

									questionsUpdate[0].answerTranscript =
										transcription + " // " + translations[0];
									callRef
										.update({
											status: "Completed",
											questions: questionsUpdate,
										})
										.then(() => {
											console.log(
												"Call " + callSID + "-- Transcription: " + transcription
											);
											if (transcription == "") {
												console.log("No transcript detected.");
												console.log(response);
											}
										});
								}

								translateText();
							}
							// delete audio recording file
							fs.unlinkSync(path);
						}
						transcribe();
					})
					.catch((err) => {
						console.log(err);
					});
			}, delay);
		});
	}
});

app.post("/status", async (req, res) => {
	const callSID = req.body.id;
	const callRef = db.collection("calls").doc(callSID);
	const call = await callRef.get();
	if (!call.exists) {
		console.log("Call " + callSID + " not found in database.");
	} else {
		let data = call.data();
		res.send(data);
	}
});

// Socket.io: receive new connections
io.on("connection", (socket) => {
	console.log("A user connected.");
	socket.emit("news", { hello: "world" });

	socket.on("call", function(data){
		console.log("callData below")
		console.log(data);

		let toPhoneNumber = "+1" + data.phoneNumber;
		let questions = data.questions;
	
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


	socket.on("others", function (data) {
		console.log(data);
	});
});

