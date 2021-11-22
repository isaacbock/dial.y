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
const clientOAUTH = new OAuth2Client(CLIENT_ID);

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
	let callerLanguage = req.body.callerLanguage;
	let businessLanguage = req.body.businessLanguage;

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
				machineDetection: "Enable",
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
			callerLanguage: callerLanguage,
			businessLanguage: businessLanguage,
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
app.post("/start", async (req, res) => {
	const callSID = req.body.CallSid;
	const answeredBy = req.body.AnsweredBy;

	if (answeredBy == "human") {
		// Find correct call in database & update status
		const callRef = db.collection("calls").doc(callSID);
		const call = await callRef.get();
		if (!call.exists) {
			console.log("Call " + callSID + " not found in database.");
		} else {
			let businessLanguage = call.data().businessLanguage;
			callRef.update({ status: "In Progress" }).then(() => {
				console.log("Call " + callSID + " in progress — Answered by human.");

				// Return starting script
				const response = new VoiceResponse();
				// response.pause({ length: 2 });
				response.say(
					{
						language: localizeLanguage(businessLanguage),
					},
					speechStrings[businessLanguage]["hi"]
				);
				response.redirect({ method: "POST" }, "/askQuestion");

				let twiml = response.toString();
				res.header("Content-Type", "application/xml");
				res.send(twiml);
			});
		}
	}
	// If answered by machine, end the call.
	else {
		db.collection("calls")
			.doc(callSID)
			.update({ status: "No Answer" })
			.then(() => {
				console.log("Call " + callSID + " in progress — Machine answer.");
				console.log("Call " + callSID + " ended automatically.");

				// End call
				const response = new VoiceResponse();
				response.hangup();

				let twiml = response.toString();
				res.header("Content-Type", "application/xml");
				res.send(twiml);
			});
	}
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
		let question = call.data().questions[0].question;
		let businessLanguage = call.data().businessLanguage;

		// Translate question to business Language
		const text = question;
		const target = businessLanguage;
		async function translateQuestion() {
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
			question = translations[0];

			let questionsUpdate = call.data().questions;
			questionsUpdate[0].status = "Asking";
			callRef.update({ questions: questionsUpdate }).then(() => {
				console.log("Call " + callSID + "-- Asking: " + question);

				// Return question script
				const response = new VoiceResponse();
				response.pause({ length: 1 });
				response.say(
					{
						language: localizeLanguage(businessLanguage),
					},
					speechStrings[businessLanguage]["wondering"]
				);
				response.say(
					{
						language: localizeLanguage(businessLanguage),
					},
					question
				);
				response.pause({ length: 1 });
				response.say(
					{
						language: localizeLanguage(businessLanguage),
					},
					speechStrings[businessLanguage]["whenReady"]
				);
				response.pause({ length: 1 });
				response.redirect({ method: "POST" }, "/promptListener");

				let twiml = response.toString();
				res.header("Content-Type", "application/xml");
				res.send(twiml);
			});
		}
		translateQuestion();
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
		let businessLanguage = call.data().businessLanguage;
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
			gather.say(
				{
					language: localizeLanguage(businessLanguage),
				},
				speechStrings[businessLanguage]["record"]
			);
			gather.say(
				{
					language: localizeLanguage(businessLanguage),
				},
				speechStrings[businessLanguage]["repeat"]
			);
			gather.say(
				{
					language: localizeLanguage(businessLanguage),
				},
				speechStrings[businessLanguage]["hangUp"]
			);
			gather.pause({ length: 5 });

			gather.say(
				{
					language: localizeLanguage(businessLanguage),
				},
				speechStrings[businessLanguage]["record"]
			);
			gather.say(
				{
					language: localizeLanguage(businessLanguage),
				},
				speechStrings[businessLanguage]["repeat"]
			);
			gather.say(
				{
					language: localizeLanguage(businessLanguage),
				},
				speechStrings[businessLanguage]["hangUp"]
			);
			gather.pause({ length: 10 });

			gather.say(
				{
					language: localizeLanguage(businessLanguage),
				},
				speechStrings[businessLanguage]["record"]
			);
			gather.say(
				{
					language: localizeLanguage(businessLanguage),
				},
				speechStrings[businessLanguage]["repeat"]
			);
			gather.say(
				{
					language: localizeLanguage(businessLanguage),
				},
				speechStrings[businessLanguage]["hangUp"]
			);
			gather.pause({ length: 10 });

			response.say(
				{
					language: localizeLanguage(businessLanguage),
				},
				speechStrings[businessLanguage]["noInput"]
			);
			response.redirect({ method: "POST" }, "/recordAnswer");

			let twiml = response.toString();
			res.header("Content-Type", "application/xml");
			res.send(twiml);
		});
	}
});

// API Call: Provide Twilio with the script to record the user's answer
app.post("/recordAnswer", async (req, res) => {
	const callSID = req.body.CallSid;

	// If no buttons were pressed, the user has hung up by now.
	if (req.body.Digits === undefined) {
		db.collection("calls")
			.doc(callSID)
			.update({ status: "Hung up" })
			.then(() => {
				console.log("Call " + callSID + "-- Hung up.");

				const response = new VoiceResponse();
				response.hangup();

				let twiml = response.toString();
				res.header("Content-Type", "application/xml");
				res.send(twiml);
			});
	}
	// If user is ready to record: update call status and start recording
	if (req.body.Digits == "1") {
		const callRef = db.collection("calls").doc(callSID);
		const call = await callRef.get();
		if (!call.exists) {
			console.log("Call " + callSID + " not found in database.");
		} else {
			let businessLanguage = call.data().businessLanguage;
			let questionsUpdate = call.data().questions;
			questionsUpdate[0].status = "Recording";
			callRef.update({ questions: questionsUpdate }).then(() => {
				console.log("Call " + callSID + "-- Recording answer.");

				const response = new VoiceResponse();
				response.say(
					{
						language: localizeLanguage(businessLanguage),
					},
					speechStrings[businessLanguage]["recordAfterBeep"]
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
	}
	// If user wants to repeat the question: redirect back to /askQuestion
	else if (req.body.Digits == "2") {
		const response = new VoiceResponse();
		response.redirect({ method: "POST" }, "/askQuestion");

		let twiml = response.toString();
		res.header("Content-Type", "application/xml");
		res.send(twiml);
	}
	// If user wants to hang up, end the call and save to Firebase
	else if (req.body.Digits == "3") {
		const callRef = db.collection("calls").doc(callSID);
		const call = await callRef.get();
		if (!call.exists) {
			console.log("Call " + callSID + " not found in database.");
		} else {
			let businessLanguage = call.data().businessLanguage;
			callRef.update({ status: "Hung Up" }).then(() => {
				console.log("Call " + callSID + "-- Hung up by listener.");

				const response = new VoiceResponse();
				response.say(
					{
						language: localizeLanguage(businessLanguage),
					},
					speechStrings[businessLanguage]["goodbye"]
				);
				let twiml = response.toString();
				res.header("Content-Type", "application/xml");
				res.send(twiml);
			});
		}
	}
	// Else prompt the user to re-input their choice
	else {
		const response = new VoiceResponse();
		response.say(
			{
				language: localizeLanguage(businessLanguage),
			},
			speechStrings[businessLanguage]["sorry"]
		);
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
		let businessLanguage = call.data().businessLanguage;
		let callerLanguage = call.data().callerLanguage;
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
				{
					language: localizeLanguage(businessLanguage),
				},
				speechStrings[businessLanguage]["recordingSaved"]
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
							const languageCode = localizeLanguage(businessLanguage);
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
								let target = callerLanguage;
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
									if (callerLanguage != businessLanguage) {
										questionsUpdate[0].answerTranscript =
											transcription + " // " + translations[0];
									} else {
										questionsUpdate[0].answerTranscript = transcription;
									}

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

// API Call: Return call history of current user
app.post("/callHistory", async (req, res) => {
	let userToken = req.body.userToken;
	// Authenticate user logged into Android app by converting their userToken into their actual user ID
	try {
		const ticket = await clientOAUTH.verifyIdToken({
			idToken: userToken,
			audience: CLIENT_ID,
		});
		const payload = ticket.getPayload();
		const userID = payload["sub"].toString();
		console.log("Getting call history of user " + userID + ".");

		let callHistory = [];
		const callRef = db.collection("calls");
		const calls = await callRef.where("user", "==", userID).get();
		if (calls.empty) {
			console.log("No calls for user " + userID + " found in database.");
		}
		calls.forEach((call) => {
			callHistory.push(call.data());
		});
		callHistory.sort((a, b) => b["date"].toDate() - a["date"].toDate());
		console.log("Call history:");
		console.log(callHistory);
		res.send(callHistory);
	} catch (error) {
		console.log(error);
	}
});

// Socket.io: receive new connections
let socketClients = new Map();
io.on("connection", (socket) => {
	console.info(`Client connected! [id=${socket.id}]`);
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
	});

	socket.on("callId", function (data) {
		socketClients.set(socket, data.phoneId);
	});

	socket.on("disconnect", () => {
		socketClients.delete(socket);
		console.info(`Client gone [id=${socket.id}]`);
	});

	socket.on("others", function (data) {
		console.log(data);
	});
});

setInterval(async () => {
	if (socketClients.size != 0) {
		for (const [client, callId] of socketClients.entries()) {
			const callSID = callId;
			const callRef = db.collection("calls").doc(callSID);
			// const call = await callRef.get();
			const call = await callRef.get();

			if (!call.exists) {
				console.log("Call " + callSID + " not found in database.");
			} else {
				let data = call.data();
				client.emit("status", data);
			}
		}
	}
}, 3000);

let speechStrings = {
	en: {
		hi: "Hi! I'm calling on behalf of a customer with a question.",
		wondering: "They're wondering,",
		whenReady:
			"When you're ready, I can record your answer to this question and send it to the customer.",
		record: "To start recording your response, press 1.",
		repeat: "To repeat their question again, press 2.",
		hangUp: "To hang up without recording a response, press 3.",
		noInput: "Sorry, we didn't receive any input. Goodbye!",
		recordAfterBeep:
			"Please record your response after the beep. When you're done recording, hang up, or press 1 to end the call.",
		goodbye: "Okay! Goodbye!",
		sorry: "Sorry, I didn't understand that.",
		recordingSaved:
			"Your recording has been saved and sent to the customer. Thank you!",
	},
	es: {
		hi: "¡Hola! Llamo en nombre de un cliente con una pregunta.",
		wondering: "Se pregunta,",
		whenReady:
			"Cuando esté listo, puedo registrar su respuesta a esta pregunta y enviársela al cliente.",
		record: "Para comenzar a grabar su respuesta, presione 1.",
		repeat: "Para repetir la pregunta nuevamente, presione 2.",
		hangUp: "Para colgar sin grabar una respuesta, presione 3.",
		noInput: "Lo sentimos, no recibimos ninguna entrada. ¡Adiós!",
		recordAfterBeep:
			"Grabe su respuesta después del tono. Cuando termine de grabar, cuelgue, o presione 1 para finalizar la llamada.",
		goodbye: "¡Okey! ¡Adiós!",
		sorry: "Lo siento, no entendí eso.",
		recordingSaved:
			"Su grabación ha sido guardada y enviada al cliente. ¡Gracias!",
	},
	fr: {
		hi: "Salut! J'appelle au nom d'un client avec une question.",
		wondering: "Il se demande,",
		whenReady:
			"Lorsque vous êtes prêt, je peux enregistrer votre réponse à cette question et l'envoyer au client.",
		record: "Pour commencer à enregistrer votre réponse, appuyez sur 1.",
		repeat: "Pour répéter à nouveau leur question, appuyez sur 2.",
		hangUp: "Pour raccrocher sans enregistrer de réponse, appuyez sur 3.",
		noInput: "Désolé, nous n'avons reçu aucune entrée. Au revoir!",
		recordAfterBeep:
			"Veuillez enregistrer votre réponse après la tonalité. Lorsque vous avez terminé l'enregistrement, raccrochez ou appuyez sur 1 pour mettre fin à l'appel.",
		goodbye: "D'accord! Au revoir!",
		sorry: "Désolé, je n'ai pas compris.",
		recordingSaved:
			"Votre enregistrement a été sauvegardé et envoyé au client. Merci!",
	},
	pt: {
		hi: "Oi! Estou ligando em nome de um cliente com uma pergunta.",
		wondering: "Ele está se perguntando",
		whenReady:
			"Quando você estiver pronto, posso registrar sua resposta a esta pergunta e enviá-la ao cliente.",
		record: "Para começar a registrar sua resposta, pressione 1.",
		repeat: "Para repetir a pergunta novamente, pressione 2.",
		hangUp: "Para desligar sem gravar uma resposta, pressione 3.",
		noInput: "Desculpe, não recebemos nenhuma entrada. Adeus!",
		recordAfterBeep:
			"Grave sua resposta após o bipe. Quando terminar a gravação, desligue ou pressione 1 para encerrar a chamada.",
		goodbye: "OK! Adeus!",
		sorry: "Desculpe, eu não entendi isso.",
		recordingSaved: "Sua gravação foi salva e enviada ao cliente. Obrigado!",
	},
};

function localizeLanguage(language) {
	if (language == "en") return "en-US";
	else if (language == "es") return "es-US";
	else if (language == "fr") return "fr-FR";
	else if (language == "pt") return "pt-BR";
	else return language;
}
