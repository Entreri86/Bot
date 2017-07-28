package services;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.logging.BotLogger;

import handlers.MNSpain_bot;

public class Poll {

	private Update update;
	private MNSpain_bot mnsBot;
	private static final String LOGTAG = "Poll";
	private int answerOptions;//numero de  preguntas
	private int [] answerScore;//puntuacion de los votos.
	private int peopleVoted;
	private int messageSurveyId;
	private String [] answers;
	private String [] callBacksData;
	private String surveyText;
	private ArrayList <String> survey;
	
	public Poll (Update update){
		this.update = update;
		peopleVoted = 0;
		mnsBot = new MNSpain_bot();
		survey = new ArrayList<String>();
	}
	
	/**
	 * 
	 * @param position
	 */
	public void addPollScore(int position){
		this.answerScore[position] = this.answerScore[position] + 1;//Aumentamos la puntuacion en 1 en la posicion dada. 
	}
	/**
	 * 
	 * @param position
	 * @return
	 */
	public int getScore (int position){
		return this.answerScore[position];
	}
	/**
	 * 
	 */
	public void peopleVotedUp(){
		this.peopleVoted = this.peopleVoted + 1; 
	}
	/**
	 * 
	 * @return
	 */
	public String [] getCallBacksData(){
		return this.callBacksData;
	}
	/**
	 * 
	 * @return
	 */
	public int getAnswersOptions (){
		return this.answerOptions;
	}
	/**
	 * 
	 * @return
	 */
	public String [] getAnswers (){
		return this.answers;
	}
	
	/**
	 * Metodo encargado de asignar la pregunta de la encuesta.
	 * @param question pregunta a tratar.
	 */
	public void setQuestion(String question){				
		this.survey.add(question);
	}
	
	/**
	 * Metodo encargado de asignar las respuestas.
	 * @param answer respuesta a tratar.
	 * @param position posicion del array donde se aloja la respuesta.
	 */
	public void setAnswers (String answer, int position){
		this.survey.add(answer);
	}
	
	/**
	 * Metodo encargado de crear la encuesta con los datos enviados por el usuario. 
	 * 
	 */
	public ArrayList<String> createSurvey (){
		final String mark = "0%";
		int count = 0;
		ArrayList<String> surveys = new ArrayList<String>();
		answerOptions = survey.size() - 1;//No necesitamos la pregunta para el conteo.
		answerScore = new int [answerOptions+1];//Iniciamos el array de puntuaciones.Quitar el -1
		answers = new String [answerOptions];//Para guardar las preguntas.		
		for (int i = 0; i < survey.size();i++){
			answerScore [i] = 0;//Marcamos a 0 las puntuaciones.
			if (i == 0){
				surveys.add(survey.get(i));//Primera pos la pregunta.
			} else {//Si es primo debe de ser una respuesta.
				surveys.add(survey.get(i));//Posible respuesta.
				answers [count] = survey.get(i);//Guardamos la pregunta para el teclado posterior.
				surveys.add(Emoji.WHITE_SMALL_SQUARE+"  "+mark);//Marca del porcentaje.EMOJI cuadrado vacio.				
				count = count +1;
			} 
		}//TODO: Posiciones 0 pregunta y 1,3,5... Respuestas. Los 2,4,6 seran las marcas porcentuales.
		surveys.add(Emoji.SLEEPY_FACE+" No ha respondido nadie todavía.");
		survey.clear();
		survey.addAll(surveys);//TO-DO OKISSS.		
		return survey;		
	}
	/**
	 * 
	 * @param position
	 * @return
	 */
	public ArrayList<String> updateSurvey (int position){
		ArrayList<String> surveys = new ArrayList<String>();
		survey.remove(survey.size()-1);//Borramos marca final.		
		final String mark = "0%";
		final String percent = "%";
		for (int i =0; i < survey.size(); i ++){//Obviamos la pregunta que no necesita ser actualizada i=1.
			if (i == 0){
				surveys.add(survey.get(i));//Pregunta
			}else{
				if (isOddNumber(i)){//si es impar
					surveys.add(survey.get(i));//respuesta
				} else{ //si es par... tiene que haber marca porcentual.
					if (getScore(i/2-1) == 0){
						surveys.add(Emoji.WHITE_SMALL_SQUARE+"  "+mark);//Marca del porcentaje.EMOJI cuadrado vacio.
					} else{
						String thumbsUp ="";
						for (int j = 0; j< getScore(i/2-1);j++){//Ponemos tantos dedos como votos haya.
							thumbsUp = thumbsUp+Emoji.THUMBS_UP_SIGN;
						}
						thumbsUp = thumbsUp + " " +getPercent(getScore(i/2-1))+percent;//Añadimos el porcentaje en todo caso
						System.out.println("Posicion: "+position+" Porcentaje "+getPercent(getScore(i/2-1))+" Puntuacion de la posicion: "+getScore(i/2-1)+" Personas que han votado: "+peopleVoted);//TODO: FALLO PORCENTAJE segunda vuelta.
						surveys.add(thumbsUp);//Añadimos a la lista.
					}
				}
			}
		}
				
		surveys.add(Emoji.PENSIVE_FACE+" "+peopleVoted+" personas han votado hasta ahora.");
		survey.clear();
		survey.addAll(surveys);
		//3 respuestas 15 votos, 1 => 3 votos = 20%, 2 => 7 votos =46,6% , 3 => 5 = 33,3% votos. % = votos / totalVotado * 100
		return survey;
	}
	
	
	
	/**
	 * Metodo encargado de convertir el ArrayList de la encuesta en un String personalizado.
	 * @param survey encuesta a convertir.
	 * @return texto personalizado.
	 */
	public String createSurveyString (ArrayList<String> survey){
		String jump = "\n";//Salto de linea.
		String inFlagBold = "<b>";//Marcas para el subrayado del texto.
		String endFlagBold = "</b>";
		String [] editedSurvey = new String [survey.size()];//Creamos array con tamaño de la List.
		editedSurvey = survey.toArray(editedSurvey);//Transformamos la lista en Array String.		
		surveyText = "";
		for (int i = 0; i< editedSurvey.length;i++){
			if (i == 0){
				surveyText = inFlagBold + surveyText + editedSurvey[i] + endFlagBold + jump + jump;//Doble salto para separar mas la pregunta de las respuestas.
			} else{
				surveyText = surveyText + editedSurvey[i] + jump;//Recogemos el texto en una variable String y le añadimos los saltos para facilitar lectura.	
			}			
		}		
		//TO-DO OKIS!
		return surveyText;
	}
	
	/**
	 * Metodo encargado de enviar la encuesta al usuario en un mensaje a parte de la contestacion.
	 * @param chatId Id del chat a donde enviar la encuesta.
	 * @param textToSend texto de la encuesta a enviar.
	 */
	public void sendSurvey (Long chatId, String textToSend){
		String parseMode = "HTML";
		String callBack = "Option";
		SendMessage message = new SendMessage();//Iniciamos mensaje y String.				
		message.setChatId(chatId);
		message.setText(textToSend);
		message.setParseMode(parseMode);//Asignamos al mensaje el parseador html para la negrita.
		//Teclado
		InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();        
        String [] answers = getAnswers();
        callBacksData = new String [getAnswersOptions()];//Iniciamos array para almacenar los callBack
        for (int i =0; i < getAnswersOptions(); i++){
        	List<InlineKeyboardButton> rowInline = new ArrayList<>();
        	InlineKeyboardButton button = new InlineKeyboardButton();
        	button.setText(answers[i]);
        	button.setCallbackData(callBack+i);
        	rowInline.add(button);        	
        	callBacksData[i] = callBack + i;
        	rowsInline.add(rowInline);            
        }        
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);        
		try {            
            Message m = mnsBot.sendMessage(message);
            messageSurveyId = m.getMessageId();//Recogemos el ID del mensaje para poder actualizarlo.            
        } catch (TelegramApiException e) {
        	BotLogger.error(LOGTAG, e);//Guardamos mensaje y lo mostramos en pantalla de la consola.
            e.printStackTrace();
        }	
		
	}
	/**
	 * 
	 * @param score
	 * @return
	 */
	private String getPercent(int score){		
		double percent = score / peopleVoted * 100;
		DecimalFormat format = new DecimalFormat("0.0");
		String finalPercent = format.format(percent);
		return finalPercent;
	}
	
	/**
	 * Metodo encargado de detectar si un numero dado por parametro es primo.
	 * @param num numero a inspeccionar.
	 * @return true en caso de ser primo, false en caso de ser par.
	 */
	public boolean isOddNumber (int num){
		if (num%2 !=0){
			return true;//es impar
		} else {
			return false;//es par
		}
	}
	
}
