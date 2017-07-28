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
	private static final int DEV_ID = 4764174;
	private static final String DEV_WORDS = "Hola creador, que necesitas de mi?";	
	private static final String START_COMMAND = "/start";
	private static final String HELP_COMMAND = "/help";
	private static final String POLL_COMMAND = "/poll";
	private static final String POLL_COMMAND_DONE = "/polldone";
	private static final String WELCOME_STRING = "Bienvenido al bot de gestión de grupos MNSpain. Este bot te ayudara en"
			+ " a gestionar tu grupo de Telegram, tu clan de Clash Royale ademas de otras funciones.\nUtiliza el comando /help para"
			+ " mas información.";
	private static final String HELP_STRING ="En construcción!";
	private static final String POLL_STRING = "Creemos la encuesta, primero envie la pregunta.";
	private static final String POLL_ANSWER_STRING = "Añada una respuesta, cuando haya añadido las respuestas deseadas pulse /polldone.";
	private static final String POLL_DONE_STRING = "Encuesta finalizada";
	private Poll poll;
	private int pollCount = 1;//Empieza en el 1 por la cuenta de la pregunta.
	private boolean isPolling = false;
	private boolean haveQuestion = false;
	private boolean sendSurvey = false;
	
	@Override
	public void onUpdateReceived(Update update) {		
		
		if (update.hasMessage() && update.getMessage().isCommand()){
			handleCommand(update.getMessage().getText(),update);
		} else if(update.hasMessage() && update.getMessage().hasText()){
			handleMessage(update.getMessage(), update);
		} else if (update.hasCallbackQuery()){
			handleCallbackQuery(update);
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
		case START_COMMAND:				
			message.setText(WELCOME_STRING);
			break;
		case HELP_COMMAND:			
			message.setText(HELP_STRING);
			break;
		case POLL_COMMAND:			
			message.setText(POLL_STRING);
			poll = new Poll(update);//Iniciamos la clase...
			isPolling = true;			
			break;
		case POLL_COMMAND_DONE:
			isPolling = false;//Reiniciamos la variable al finalizar el comando.
			haveQuestion = false;//Reiniciamos la variable para la pregunta.
			sendSurvey = true;//Marcamos para enviar la encuesta.
			pollCount = 0;//Reiniciamos contador.			
			message.setText(POLL_DONE_STRING);
			break;
		
		}		
		try {
			message.setChatId(chatId);
            sendMessage(message); // Call method to send the message
            if (sendSurvey == true){
            	poll.sendSurvey(chatId,poll.createSurveyString(poll.createSurvey()));//Si hay que enviar la encuesta...
            	sendSurvey = false;//Marcamos como no enviada despues de haberlo hecho.
            }
        } catch (TelegramApiException e) {
        	BotLogger.error(LOGTAG, e);//Guardamos mensaje y lo mostramos en pantalla de la consola.
            e.printStackTrace();
        }
	}
	
	/**
	 * 
	 * @param message
	 * @param update
	 */
	private void handleMessage (Message message, Update update){
		SendMessage sendMessage = new SendMessage();
		Long chatId = update.getMessage().getChatId();
		if (isPolling == true){//Si el comando de la encuesta ha sido pulsado, modo encuesta...			
			if (haveQuestion == false){//Si es falso todavia no se ha asignado la pregunta...
				poll.setQuestion(message.getText());//Asignamos	la pregunta.			
				sendMessage.setChatId(chatId);
				sendMessage.setText("Pregunta añadida. "+POLL_ANSWER_STRING);
				haveQuestion = true;//Marcamos que hay pregunta.
			} else {//En este estado tenemos la pregunta, asignamos las respuestas.
				poll.setAnswers(message.getText(),pollCount);
				pollCount += 1;
				sendMessage.setChatId(chatId);
				sendMessage.setText(POLL_ANSWER_STRING);
			}			
		} else if(update.getMessage().getFrom().getId() != null){
			Integer id = update.getMessage().getFrom().getId();
			if (id == DEV_ID){
				sendMessage.setChatId(chatId);
				sendMessage.setText(DEV_WORDS);
			}	
		} else {
			sendMessage.setChatId(chatId);
			sendMessage.setText(update.getMessage().getText());
		}        		
        try {
        	
            sendMessage(sendMessage); // Call method to send the message
        } catch (TelegramApiException e) {
        	BotLogger.error(LOGTAG, e);//Guardamos mensaje y lo mostramos en pantalla de la consola.
            e.printStackTrace();
        }
	}
	
	private void handleCallbackQuery (Update update){
		Long chatId = update.getCallbackQuery().getMessage().getChatId();
		String call_data = update.getCallbackQuery().getData();
		String [] callBackData = poll.getCallBacksData();
		int pos = 0;
		for (int i = 0; i < poll.getAnswersOptions(); i++){
			if (callBackData [i].equals(call_data)){
				pos = i;//Guardamos la posicion.
				poll.addPollScore(pos);//Aumentamos la puntuacion.
				poll.peopleVotedUp();//Subimos el conteo de gente que ha votado.				
			}
		}
		poll.sendSurvey(chatId, poll.createSurveyString(poll.updateSurvey(pos)));
		
		
		
		
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
