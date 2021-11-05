// Import API keys
require("dotenv").config({ path: __dirname + `/../.env` });

// Import Express
const express = require("express");
const app = express();
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.json());
let port = process.env.PORT || 3000;

// Import Twilio
const VoiceResponse = require("twilio").twiml.VoiceResponse;
const accountSid = process.env.TWILIO_ACCOUNT_SID;
const authToken = process.env.TWILIO_AUTH_TOKEN;
const client = require("twilio")(accountSid, authToken);

// Import Google OAuth
const { OAuth2Client } = require("google-auth-library");
let CLIENT_ID =
	"984298290533-4hqf6oj0gqmk0jkjpg65f7u577t9flg6.apps.googleusercontent.com";

// Import Firebase
const admin = require("firebase-admin");
const serviceAccount = JSON.parse(
	process.env.GOOGLE_APPLICATION_CREDENTIALS_FIREBASE
);
admin.initializeApp({
	credential: admin.credential.cert(serviceAccount),
});
let db = admin.firestore();

//Import Socket.io
const http = require("http");
const server = http.createServer(app);
const { Server } = require("socket.io");
const io = new Server(server);
server.listen(port, () => {
	console.log(`Listening on port ${port}.`);
});

// Import Google Cloud Speech
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

// Import Google Translate
const { Translate } = require("@google-cloud/translate").v2;
const translate = new Translate();

// API Calls
app.get("/", (req, res) => {
	res.send("Hello world.");
});

// API Call: Start a new call
app.post("/call", async (req, res) => {
	let toPhoneNumber = "+1" + req.body.phoneNumber;
	let questions = req.body.questions;
	let userToken = req.body.userToken;

	try {
		// Authenticate user logged into Android app by converting their userToken into their actual user ID
		const ticket = await clientOAUTH.verifyIdToken({
			idToken: userToken,
			audience: CLIENT_ID,
		});
		const payload = ticket.getPayload();
		const userID = payload["sub"];
		console.log("Call created from User ID: " + userID);

		// Start call via Twilio
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

		// Create data structure to track and store call
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
			user: userID,
			status: "Dialing",
			date: new Date(),
			questions: callQuestions,
		};

		// Save call to Firebase
		db.collection("calls")
			.doc(callSID)
			.set(call)
			.then(() => {
				console.log("Call " + callSID + " added to database.");
				res.send(callSID);
			});
	} catch (error) {
		console.log(error);
	}
});

// API Call: Provide Twilio with the call introduction script
app.post("/start", (req, res) => {
	const callSID = req.body.CallSid;

	// Find correct call in database & update status
	db.collection("calls")
		.doc(callSID)
		.update({ status: "In Progress" })
		.then(() => {
			console.log("Call " + callSID + " in progress.");

			// Return starting script
			const response = new VoiceResponse();
			response.pause({ length: 2 });
			response.say("Hi! I'm calling on behalf of a customer with a question.");
			response.redirect({ method: "POST" }, "/askQuestion");

			let twiml = response.toString();
			res.header("Content-Type", "application/xml");
			res.send(twiml);
		});
});

// API Call: Provide Twilio with the script to ask a question
app.post("/askQuestion", async (req, res) => {
	const callSID = req.body.CallSid;

	// Find correct call in database & update status
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

			// Return question script
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

// API Call: Provide Twilio with the script to prompt the listener to record their answer
app.post("/promptListener", async (req, res) => {
	const callSID = req.body.CallSid;

	// Find correct call in database & update status
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

			// Return prompting script
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

// API Call: Provide Twilio with the script to record the user's answer
app.post("/recordAnswer", async (req, res) => {
	const callSID = req.body.CallSid;

	if (req.body.Digits === undefined) {
		req.body.Digits = 0;
	}

	// If user is ready to record: update call status and start recording
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
		// If user wants to repeat the question: redirect back to /askQuestion
	} else if (req.body.Digits == "2") {
		const response = new VoiceResponse();
		response.redirect({ method: "POST" }, "/askQuestion");

		let twiml = response.toString();
		res.header("Content-Type", "application/xml");
		res.send(twiml);
		// If user wants to hang up, end the call and save to Firebase
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
		// Else prompt the user to re-input their choice
	} else {
		const response = new VoiceResponse();
		response.say("Sorry, I didn't understand that.");
		response.redirect({ method: "POST" }, "promptListener");

		let twiml = response.toString();
		res.header("Content-Type", "application/xml");
		res.send(twiml);
	}
});

// API Call: Save Twilio recording URL & transcribe & translate the results
app.post("/saveRecording", async (req, res) => {
	const callSID = req.body.CallSid;

	// Find correct call in database & update status
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

			// Provide call ending script
			const response = new VoiceResponse();
			response.say(
				"Your recording has been saved and sent to the customer. Thank you!"
			);
			let twiml = response.toString();
			res.header("Content-Type", "application/xml");
			res.send(twiml);

			// Transcribe audio recording:
			const url = req.body.RecordingUrl;
			const path = toString(callSID) + ".wav";
			// Download audio file for transcription (wait small delay first to ensure that it is available on Twilio)
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
						// Transcribe audio recording via Google Cloud Speech
						async function transcribe() {
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
							// Detect speech in the audio file
							const [response] = await client.recognize(request);
							const transcription = response.results
								.map((result) => result.alternatives[0].transcript)
								.join("\n");

							// Update call in database to include transcription results
							call = await callRef.get();
							if (!call.exists) {
								console.log("Call " + callSID + " not found in database.");
							} else {
								let questionsUpdate = call.data().questions;
								questionsUpdate[0].status = "Completed";

								// Translate results to target language
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

									// Update call in database to include translation results
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

							// Delete audio recording file after completion
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

// API Call: Return status of specified call in order to update the frontend
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

	socket.on("call", function (data) {
		console.log("callData below");
		console.log(data);

		let toPhoneNumber = "+1" + data.phoneNumber;
		console.log("phone number from socket");
		console.log(toPhoneNumber);
		let questions = data.questions;
		console.log("Questions from socket");
		console.log(questions);

		// callSID = await client.calls
		// .create({
		// 	url: "https://cse437s-phone.herokuapp.com/start",
		// 	to: toPhoneNumber,
		// 	from: "+15153165732",
		// })
		// .then((call) => {
		// 	console.log("Call " + call.sid + " initiated.");
		// 	return call.sid;
		// });

		// let callQuestions = [];
		// for (question of questions) {
		// 	let newQuestion = {
		// 		question: question,
		// 		status: "Waiting",
		// 		answerAudio: null,
		// 		answerTranscript: null,
		// 	};
		// 	callQuestions.push(newQuestion);
		// }
		// const call = {
		// 	to: toPhoneNumber,
		// 	status: "Dialing",
		// 	date: new Date(),
		// 	questions: callQuestions,
		// };
		// db.collection("calls")
		// 	.doc(callSID)
		// 	.set(call)
		// 	.then(() => {
		// 		console.log("Call " + callSID + " added to database.");
		// 		socket.emit("callId", callSID)
		// 	});
	});

	socket.on("others", function (data) {
		console.log(data);
	});
});

let token =
	"eyJhbGciOiJSUzI1NiIsImtpZCI6IjY5NGNmYTAxOTgyMDNlMjgwN2Q4MzRkYmE2MjBlZjczZjI4ZTRlMmMiLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiSXNhYWMgQm9jayIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS0vQU9oMTRHaVJZeEFSMFAycmVYVUZObFBDZF9XMHd6V0FRTWlsMDhtVXItb3g5QT1zOTYtYyIsImlzcyI6Imh0dHBzOi8vc2VjdXJldG9rZW4uZ29vZ2xlLmNvbS9waG9uZS1hcHAtODA2ZGUiLCJhdWQiOiJwaG9uZS1hcHAtODA2ZGUiLCJhdXRoX3RpbWUiOjE2MzYwODI0MjMsInVzZXJfaWQiOiJXWlNWR0oweTlHV0FJUEhZQVJLSzJTVnRaZlYyIiwic3ViIjoiV1pTVkdKMHk5R1dBSVBIWUFSS0syU1Z0WmZWMiIsImlhdCI6MTYzNjA4MjQyMywiZXhwIjoxNjM2MDg2MDIzLCJlbWFpbCI6ImlzYWFjYm9ja0BnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiZmlyZWJhc2UiOnsiaWRlbnRpdGllcyI6eyJnb29nbGUuY29tIjpbIjEwMTE1MDIzNzg3MjI1MjMwMDQzMCJdLCJlbWFpbCI6WyJpc2FhY2JvY2tAZ21haWwuY29tIl19LCJzaWduX2luX3Byb3ZpZGVyIjoiZ29vZ2xlLmNvbSJ9fQ.FKntyVAuIlZXZZLZFDxpXC1Z07tfcL2e7gs8SnwGb4fChf6U9SQH7OU_M6m5KpTER-oq4PLlIJvU41CAT8xJdlhrBZ4YihcRbV740WiRdpWMACMMU6_FiZ_13lBJnEFaAjgpy7pQEt7BZLLFtEkm35Uw4tp4PtaBWRgzJD_ZK-CCexrLEWiQGX54HQaM5JP-Bmr1BpC03_xRA9bBCQGwJRtgLZmXNJduonzc0p--uKtwUXS0o3xU5yVm2fh2gRAgS0PVmuBGXBmw7mgiHXeWpbE3af8GvGfhSaUnG-B7rGVimuMXyFGgDn_fjCesTn8F_W6L7T7z4UAdgEl_L2ngGg";
const { OAuth2Client } = require("google-auth-library");
let CLIENT_ID =
	"984298290533-4hqf6oj0gqmk0jkjpg65f7u577t9flg6.apps.googleusercontent.com";
const clientOAUTH = new OAuth2Client(CLIENT_ID);
async function verify() {
	const ticket = await clientOAUTH.verifyIdToken({
		idToken: token,
		audience: CLIENT_ID,
	});
	const payload = ticket.getPayload();
	const userid = payload["sub"];
	console.log("USERID: " + userid);
}
verify(token).catch(console.error);
