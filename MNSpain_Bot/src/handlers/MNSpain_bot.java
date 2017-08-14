package handlers;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.logging.BotLogger;
import bot.BotConfig;
import services.Poll;


public class MNSpain_bot extends TelegramLongPollingBot {
	private static final String LOGTAG = "MNSpain";		
	private Poll poll;	
	private boolean isPolling = false;
	private boolean haveQuestion = false;
	private boolean sendSurvey = false;
	
	/**
	 * Metodo encargado de gestionar y derivar las actualizaciones que le llegan al bot.
	 */
	@Override
	public void onUpdateReceived(Update update) {			
		if (update.hasMessage() && update.getMessage().isCommand()){//Si es un comando...
			handleCommand(update.getMessage().getText(),update);
		} else if(update.hasMessage() && update.getMessage().hasText()){//Si es un mensaje...
			handleMessage(update.getMessage(), update);
		} else if (update.hasCallbackQuery()){//Si es una pulsacion de boton de un teclado...
			handleCallbackQuery(update);
		} else if (update.hasInlineQuery()){//Si es una consulta inline...
			handleInlineQuery(update);
		}
		
	}
	
	/**
	 * Metodo encargado de gestionar los comandos dirijidos por los usuarios al bot.
	 * @param command comando a gestionar.
	 * @param update actualizacion de estado de Telegram.
	 */
	private void handleCommand(String command, Update update){
		SendMessage message= new SendMessage();		
		Long chatId = update.getMessage().getChatId();
		switch (command){
		case BotConfig.START_COMMAND:				
			message.setText(BotConfig.WELCOME_STRING);
			break;
		case BotConfig.HELP_COMMAND:			
			message.setText(BotConfig.HELP_STRING);
			break;
		case BotConfig.POLL_COMMAND:			
			message.setText(BotConfig.POLL_STRING);
			poll = new Poll();//Iniciamos la clase...
			isPolling = true;//"Encendemos" el modo encuesta.			
			break;
		case BotConfig.POLL_COMMAND_DONE:
			isPolling = false;//Reiniciamos la variable al finalizar el comando.
			haveQuestion = false;//Reiniciamos la variable para la pregunta.
			sendSurvey = true;//Marcamos para enviar la encuesta.				
			message.setText(BotConfig.POLL_DONE_STRING);
			break;
		
		}		
		try {
			message.setChatId(chatId);
			execute(message);//Enviamos el mensaje...            
            if (sendSurvey == true){            	
            	execute(poll.sendFinishedSurvey(chatId, poll.createSurveyString(poll.createSurvey())));//Enviamos encuesta antes de compartir.
            	sendSurvey = false;//Marcamos como no enviada despues de haberlo hecho.
            }
        } catch (TelegramApiException e) {
        	BotLogger.error(LOGTAG, e);//Guardamos mensaje y lo mostramos en pantalla de la consola.
            e.printStackTrace();
        }
	}	
	/**
	 * Metodo encargado de gestionar los mensajes que llegan al bot.
	 * @param message Mensaje del chat.
	 * @param update actualizacion de estado.
	 */
	private void handleMessage (Message message, Update update){
		SendMessage sendMessage = new SendMessage();
		Long chatId = update.getMessage().getChatId();
		if (isPolling == true){//Si el comando de la encuesta ha sido pulsado, modo encuesta...			
			if (haveQuestion == false){//Si es falso todavia no se ha asignado la pregunta...
				poll.setQuestion(message.getText());//Asignamos	la pregunta.			
				sendMessage.setChatId(chatId);
				sendMessage.setParseMode(Poll.parseMode);
				sendMessage.setText(BotConfig.POLL_QUESTION_STRING+ message.getText() +BotConfig.POLL_FIRST_ANSWER_STRING);
				haveQuestion = true;//Marcamos que hay pregunta.
			} else {//En este estado tenemos la pregunta, asignamos las respuestas.
				poll.setAnswers(message.getText());				
				sendMessage.setChatId(chatId);
				sendMessage.setText(BotConfig.POLL_ANSWER_STRING);
			}			
		} else if(update.getMessage().getFrom().getId() != null){//Si el id del usuario no es null...
			Integer id = update.getMessage().getFrom().getId();
			if (id == BotConfig.DEV_ID){//Si es mi id...
				sendMessage.setChatId(chatId);
				sendMessage.setText(BotConfig.DEV_WORDS);//Mensaje personalizado...xD
			}	
		} else {//Sino respondemos con el mismo texto enviado por el usuario.
			sendMessage.setChatId(chatId);
			sendMessage.setText(update.getMessage().getText());
		}        		
        try {        	
            execute(sendMessage);//Enviamos mensaje. 
        } catch (TelegramApiException e) {
        	BotLogger.error(LOGTAG, e);//Guardamos mensaje y lo mostramos en pantalla de la consola.
            e.printStackTrace();
        }
	}
	
	/**
	 * Metodo encargado de gestionar las CallBackQueries que puedan llegar al bot.
	 * @param update actualizacion del estado.
	 */
	private void handleCallbackQuery (Update update){		
		String chatId = update.getCallbackQuery().getInlineMessageId();//Id del chat.
		Integer userId = update.getCallbackQuery().getFrom().getId();//Id del usuario a controlar!!!
		System.out.println("ID del usuario a controlar: "+userId);
		String call_data = update.getCallbackQuery().getData();//Datos del boton pulsado.		
		boolean isOnList = poll.isOnList(userId);//Miramos si el usuario esta en la lista.
		if (isOnList){//Si esta en la lista de votos...
			try {
				int pos = poll.getCallbackPos(call_data);//Recogemos la posicion del boton pulsado.				
				poll.reducePollScore(userId);//Reducimos el voto introducido anteriormente.
				poll.addPollScore(pos, userId);//Ponemos el voto en la posicion nueva.
				String text = "Cambio de voto registrado correctamente.";
				execute(poll.replyVote(update.getCallbackQuery().getId(),text));//Mensaje informativo...
				execute(poll.updateMessage(chatId, poll.createSurveyString(poll.updateSurvey(pos))));//Actualizamos la encuesta.
			} catch (TelegramApiException e) {
				BotLogger.error(LOGTAG, e);//Guardamos mensaje y lo mostramos en pantalla de la consola.
				e.printStackTrace();
			}
		} else {//Si no ha votado todavia...
			int pos = poll.getCallbackPos(call_data);//Recogemos la posicion del boton pulsado.			
			poll.addPollScore(pos,userId);//Aumentamos la puntuacion en la posicion dada.
			try {
				String text = "Voto registrado correctamente.";
				execute(poll.replyVote(update.getCallbackQuery().getId(),text));//Mensaje informativo...
				execute(poll.updateMessage(chatId, poll.createSurveyString(poll.updateSurvey(pos))));//Actualizamos la encuesta.						
			} catch (TelegramApiException e) {
				BotLogger.error(LOGTAG, e);//Guardamos mensaje y lo mostramos en pantalla de la consola.
				e.printStackTrace();
			}	
		}		
	}
	/**
	 * Metodo encargado de gestionar las InlineQueries.
	 * @param update actualizacion del estado.
	 */
	private void handleInlineQuery (Update update){
		try {
			execute(poll.convertToAnswerInlineQuery(update.getInlineQuery()));//Contestamos a la inlineQuery compartiendo la encuesta.				
		} catch (TelegramApiException e) {
			BotLogger.error(LOGTAG, e);//Guardamos mensaje y lo mostramos en pantalla de la consola.
			e.printStackTrace();
		}
	}
	/**
	 * Metodo que devuelve el nombre del bot dado a Botfather.
	 */
	@Override
	public String getBotUsername() {		
		return BotConfig.BOT_USER_NAME;
	}

	/**
	 * Metodo que devuelve el token asignado por Botfather.
	 */
	@Override
	public String getBotToken() {		
		return BotConfig.BOT_TOKEN;
	}	
}
