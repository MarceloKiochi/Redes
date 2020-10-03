import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

//classe utilizada para simular a temperatura
//temperatura foi simulada a partir de arquivos de leitura e escrita

public class Temperatura extends Thread{
	private static String pathTemperatura = "temperatura.txt";/*nome do arquivo que simula temperatura*/
	private static String pathContribuicaoAquecedor = "contribuicaoAquecedor.txt";	//arquivo de contribuicao do aquecedor
	private static String pathContribuicaoResfriador = "contribuicaoResfriador.txt";//contribuicao do resfriador
	private static String pathTemperaturaAmbiente = "temperaturaAmbiente.txt";	//nome do arquivo que simula temperatura fora da estufa
	
	//arquivos
	private static File arqTemperatura = null;
	private static File arqContribuicaoAquecedor = null;
	private static File arqContribuicaoResfriador = null;
	private static File arqTemperaturaAmbiente = null;
	
	private static int contribuicaoTemperaturaAmbiente = 0;	//contribuicao da temperatura ambiente (fora da estufa)
	private static int TemperaturaAmbiente = 0;	//valor da temperatura ambiente

	/* Metodo que retorna o arquivo de temperatura para leitura ou escrita*/
	public static File getArqTemperatura() throws IOException {
		if(arqTemperatura == null)
			createFileTemperatura();
		return arqTemperatura;
	}
	
	/* Metodo que retorna o arquivo de contribuicao do aquecedor para leitura ou escrita*/
	public static File getArqContribuicaoAquecedor() throws IOException {
		if(arqContribuicaoAquecedor == null)
			createFileContribuicaoAquecedor();
		return arqContribuicaoAquecedor;
	}
	
	//retorna arquivo de contribuicao do resfriador
	public static File getArqContribuicaoResfriador() throws IOException {
		if(arqContribuicaoResfriador == null)
			createFileContribuicaoResfriador();
		return arqContribuicaoResfriador;
	}
	
	//retorna arquivo de temperatura ambiente (fora da estufa);
	public static File getArqTemperaturaAmbiente() throws IOException {
		if(arqTemperaturaAmbiente == null)
			createFileTemperaturaAmbiente();
		return arqTemperaturaAmbiente;
	}
	
	//metodo que muda o fator de contribuicao do aquecedor (liga ou desliga)
	//contribuicao: eh o numero de unidades com que um atuador muda o valor do seu parametro
	//contribuicao do aquecedor = 2. Significa que o aquecedor aumenta a temperatura de 2 em 2 (aquecedor ligado)
	//contribuicao do aquecedor = 0. Significa que o aquecedor nao aumenta nem diminui a temperatura (desligado)
	public static void setContribuicaoAquecedor(Integer alteracao) {
		try {
			FileWriter fw = new FileWriter(getArqContribuicaoAquecedor());	//arquivo de contribuicao do aquecedor
			BufferedWriter buffWrite = new BufferedWriter(fw);				//escritor
			buffWrite.append(alteracao.toString() + String.valueOf('\n'));	//escreve o novo valor da contribuicao
			buffWrite.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//metodo que muda o fator de contribuicao do resfriador (liga ou desliga)
	//contribuicao: eh o numero de unidades com que um atuador muda o valor do seu parametro
	//contribuicao do resfriador = -2. Significa que o resfriador diminui a temperatura de 2 em 2 (resfriador ligado)
	//contribuicao do resfriador = 0. Significa que o resfriador nao aumenta nem diminui a temperatura (desligado)
	public static void setContribuicaoResfriador(Integer alteracao) {
		try {
			FileWriter fw = new FileWriter(getArqContribuicaoResfriador());//arquivo de contribuicao do resfriador
			BufferedWriter buffWrite = new BufferedWriter(fw);	//escritor
			buffWrite.append(alteracao.toString() + String.valueOf('\n'));	//escreve novo valor de contribuicao
			buffWrite.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* Esse metodo realiza a criacao do arquivo de temperatura(utilizado para simular a temperatura) caso ele nao exista
	 * Caso ele ja se encontre criado eh feito uma checagem no arquivo, verificando se ele esta vazio(se isto ocorrer passa o valor default)
	 * Se ja se encontrar dados no arquivo entao nao eh feito nada*/
	private static void createFileTemperatura() throws IOException {
		Integer defaultTemperatura = 0;
		arqTemperatura = new File(pathTemperatura);
		if(!arqTemperatura.exists()) {
			arqTemperatura.createNewFile();
			FileWriter fw = new FileWriter(getArqTemperatura());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(defaultTemperatura.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma temperatura Inicial*/
			buffWrite.close();
		}else {
			FileReader fr = new FileReader(getArqTemperatura());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio*/
				FileWriter fw = new FileWriter(arqTemperatura);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(defaultTemperatura.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma temperatura Inicial*/
				buffWrite.close();
			}
			buffRead.close();
		}
	}
	
	/* Esse metodo realiza a criacao do arquivo de contribuicao do aquecedor caso ele nao exista
	 * Caso ele ja se encontre criado eh feito uma checagem no arquivo, verificando se ele esta vazio(se isto ocorrer passa o valor default)
	 * Se ja se encontrar dados no arquivo entao nao eh feito nada*/
	private static void createFileContribuicaoAquecedor() throws IOException{
		Integer contribuicaoDefault = 0;
		arqContribuicaoAquecedor = new File(pathContribuicaoAquecedor);
		if(!arqContribuicaoAquecedor.exists()) {
			arqContribuicaoAquecedor.createNewFile();
			FileWriter fw = new FileWriter(getArqContribuicaoAquecedor());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));
			buffWrite.close();
		}else {//Se arquivo existir
			FileReader fr = new FileReader(getArqContribuicaoAquecedor());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio(modificado)*/
				FileWriter fw = new FileWriter(arqContribuicaoAquecedor);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma contribuicao Inicial*/
				buffWrite.close();
			}
			buffRead.close();
		}
	}
	
	/* Esse metodo realiza a criacao do arquivo de contribuicao do resfriador caso ele nao exista
	 * Caso ele ja se encontre criado eh feito uma checagem no arquivo, verificando se ele esta vazio(se isto ocorrer passa o valor default)
	 * Se ja se encontrar dados no arquivo entao nao eh feito nada*/
	private static void createFileContribuicaoResfriador() throws IOException{
		Integer contribuicaoDefault = 0;
		arqContribuicaoResfriador = new File(pathContribuicaoResfriador);
		if(!arqContribuicaoResfriador.exists()) {
			arqContribuicaoResfriador.createNewFile();
			FileWriter fw = new FileWriter(getArqContribuicaoResfriador());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));
			buffWrite.close();
		}else {//Se arquivo existir
			FileReader fr = new FileReader(getArqContribuicaoResfriador());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio(modificado)*/
				FileWriter fw = new FileWriter(arqContribuicaoResfriador);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma contribuicao Inicial*/
				buffWrite.close();
			}
			buffRead.close();
		}
	}
	
	/* Esse metodo realiza a criacao do arquivo de temperatura ambiente caso ele nao exista
	 * Caso ele ja se encontre criado eh feito uma checagem no arquivo, verificando se ele esta vazio(se isto ocorrer passa o valor default)
	 * Se ja se encontrar dados no arquivo entao nao eh feito nada*/
	private static void createFileTemperaturaAmbiente() throws IOException{
		Integer TemperaturaAmbienteDefault = 0;
		arqTemperaturaAmbiente = new File(pathTemperaturaAmbiente);
		if(!arqTemperaturaAmbiente.exists()) {
			arqTemperaturaAmbiente.createNewFile();
			FileWriter fw = new FileWriter(getArqTemperaturaAmbiente());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(TemperaturaAmbienteDefault.toString() + String.valueOf('\n'));
			buffWrite.close();
		}else {//Se arquivo existir
			FileReader fr = new FileReader(getArqTemperaturaAmbiente());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio(modificado)*/
				FileWriter fw = new FileWriter(arqTemperaturaAmbiente);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(TemperaturaAmbienteDefault.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma temperatura Inicial*/
				buffWrite.close();
			}
			buffRead.close();
		}
	}
	
	/* Esse metodo obtem a temperatura atual lendo o arquivo temperatura.txt
	 * E pega os fatores de contribuicao do aquecedor e do resfriador
	 * Tendo esses valores eh aplicado o fator de contribuicao do ambiente
	 * E retornado a temperatura atualizada*/
	private int updateTemperatura() throws FileNotFoundException, IOException{
		FileReader fr = new FileReader(getArqTemperatura());
		BufferedReader buffRead = new BufferedReader(fr);
		
		/*Lendo a temperatura do arquivo*/
		Integer temperaturaAtual = Integer.parseInt(buffRead.readLine());
		
		fr = new FileReader(getArqTemperaturaAmbiente());
		buffRead = new BufferedReader(fr);
		TemperaturaAmbiente = Integer.parseInt(buffRead.readLine());	//leitura da temperatura ambiente (fora da estufa)
		
		//se fora da estufa estiver mais quente, fator de contribuicao da temperatura ambiente sera 1 (aumenta o da estufa)
		if(temperaturaAtual < TemperaturaAmbiente)
			contribuicaoTemperaturaAmbiente = 1;
		else if(temperaturaAtual > TemperaturaAmbiente)	//se estiver mais frio, diminui
			contribuicaoTemperaturaAmbiente = -1;
		else	//se estiver igual, nao altera
			contribuicaoTemperaturaAmbiente = 0;
		
		//a temperatura ambiente altera sempre de 1 em 1, enquanto os atuadores alteram de 2 em 2
		//Portanto, os atuadores conseguem sempre manipular a temperatura da estufa, pois se sobrepoem a temperatura ambiente
		
		/*Lendo contribuicao dos atuadores nos arquivos*/
		fr = new FileReader(getArqContribuicaoAquecedor());
		buffRead = new BufferedReader(fr);
		Integer contribuicaoAquecedor = Integer.parseInt(buffRead.readLine());//contribuicao do aquecedor
		
		fr = new FileReader(getArqContribuicaoResfriador());
		buffRead = new BufferedReader(fr);
		Integer contribuicaoResfriador = Integer.parseInt(buffRead.readLine());//contribuicao do resfriador
		
		buffRead.close();
		
		return temperaturaAtual + contribuicaoTemperaturaAmbiente + contribuicaoAquecedor + contribuicaoResfriador;
	}
	
	//atualiza a temperatura a cada segundo
	@Override
	public void run() {
		Integer temperaturaAtual;
		FileWriter fw = null;
		BufferedWriter buffWrite = null;
		while(true) {
			try {
				TimeUnit.SECONDS.sleep(1);
				temperaturaAtual = updateTemperatura();	//pega novo valor da temperatura
				
				//escreve novo valor no arquivo de temperatura
				fw = new FileWriter(getArqTemperatura());
				buffWrite = new BufferedWriter(fw);
				buffWrite.append(temperaturaAtual.toString() + '\n');
				buffWrite.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				System.out.println("Problema na formatacao dos dados da temperatura");
				return;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
	}
}
