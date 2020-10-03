import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

//classe utilizada para simular o nivel de CO2
//nivel de CO2 foi simulado a partir de arquivos de leitura e escrita

public class CO2 extends Thread{
	private static String pathCO2 = "co2.txt";/*arquivo que simula co2*/
	private static String pathContribuicaoCO2 = "contribuicaoCO2.txt";	//arquivo de contribuicao do injetor
	private static File arqCO2 = null;
	private static File arqContribuicaoCO2 = null;
	private static int contribuicaoCO2Ambiente = -1;	//ambiente (fora da estufa) sempre diminui em 1 o nivel de CO2
	private static int timeUpdate = 1;	//tempo para atualizar o valor do nivel de CO2 (segundos)

	/* Metodo que retorna o arquivo de co2 para leitura ou escrita*/
	public static File getArqCO2() throws IOException {
		if(arqCO2 == null)
			createFileCO2();
		return arqCO2;
	}
	
	/* Metodo que retorna o arquivo de contribuicao para leitura ou escrita*/
	public static File getArqContribuicaoCO2() throws IOException {
		if(arqContribuicaoCO2 == null)
			createFileContribuicaoCO2();
		return arqContribuicaoCO2;
	}
	
	//metodo que muda o fator de contribuicao do injetor (liga ou desliga)
	//contribuicao: eh o numero de unidades com que um atuador muda o valor do seu parametro
	//contribuicao do injetor = 2. Significa que o injetor aumenta o nivel de CO2 de 2 em 2 (ligado)
	//contribuicao do injetor = 0. Significa que o injetor nao aumenta nem diminui o nivel de CO2 (desligado)
	public static void setContribuicaoCO2(Integer alteracao) {
		try {
			FileWriter fw = new FileWriter(getArqContribuicaoCO2());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(alteracao.toString() + String.valueOf('\n'));
			buffWrite.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* Esse metodo realiza a criacao do arquivo de co2(utilizado para simular o co2) caso ele nao exista
	 * Caso ele ja se encontre criado eh feito uma checagem no arquivo, verificando se ele esta vazio(se isto ocorrer passa o valor default)
	 * Se ja se encontrar dados no arquivo entao nao eh feito nada*/
	private static void createFileCO2() throws IOException {
		Integer defaultCO2 = 300;
		arqCO2 = new File(pathCO2);
		if(!arqCO2.exists()) {
			arqCO2.createNewFile();
			FileWriter fw = new FileWriter(getArqCO2());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(defaultCO2.toString() + String.valueOf('\n'));/*Inicializa o arquivo com um nivel de co2 Inicial*/
			buffWrite.close();
		}else {
			FileReader fr = new FileReader(getArqCO2());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio*/
				FileWriter fw = new FileWriter(arqCO2);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(defaultCO2.toString() + String.valueOf('\n'));/*Inicializa o arquivo com um nivel de co2 Inicial*/
				buffWrite.close();
			}
			buffRead.close();
		}
	}
	
	/* Esse metodo realiza a criacao do arquivo de contribuicao(utilizado para simular a contribuicao do atuador no co2) caso ele nao exista
	 * Caso ele ja se encontre criado eh feito uma checagem no arquivo, verificando se ele esta vazio(se isto ocorrer passa o valor default)
	 * Se ja se encontrar dados no arquivo entao nao eh feito nada*/
	private static void createFileContribuicaoCO2() throws IOException{
		Integer contribuicaoDefault = 0;
		arqContribuicaoCO2 = new File(pathContribuicaoCO2);
		if(!arqContribuicaoCO2.exists()) {
			arqContribuicaoCO2.createNewFile();
			FileWriter fw = new FileWriter(getArqContribuicaoCO2());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));
			buffWrite.close();
		}else {//Se arquivo existir
			FileReader fr = new FileReader(getArqContribuicaoCO2());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio(modificado)*/
				FileWriter fw = new FileWriter(arqContribuicaoCO2);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma contribuicao Inicial*/
				buffWrite.close();
			}
			buffRead.close();
		}
	}
	
	/* Esse metodo obtem o co2 atual lendo o arquivo co2.txt
	 * E pega o fator de contribuicao do injetor
	 * Tendo esses valores eh aplicado o fator de contribuicao do ambiente
	 * E retornado o nivel de co2 atualizado*/
	private int updateCO2() throws FileNotFoundException, IOException{
		FileReader fr = new FileReader(getArqCO2());
		BufferedReader buffRead = new BufferedReader(fr);
		Integer contribuicaoCO2Equip;
		/*Lendo o co2 do arquivo*/
		Integer CO2Atual = Integer.parseInt(buffRead.readLine());
		
		if(CO2Atual <= 0) {	//quando nivel de co2 chega a 0, para de diminuir
			contribuicaoCO2Ambiente = 0;
		} else {
			contribuicaoCO2Ambiente = -1;
		}
				
		/*Lendo contribuicao do injetor no arquivo*/
		fr = new FileReader(getArqContribuicaoCO2());
		buffRead = new BufferedReader(fr);
		contribuicaoCO2Equip = Integer.parseInt(buffRead.readLine());
		
		buffRead.close();
		
		return CO2Atual + contribuicaoCO2Ambiente + contribuicaoCO2Equip;
	}
	
	//atualiza o valor do nivel de CO2 a cada intervalo de tempo timeUpdate
	@Override
	public void run() {
		Integer co2Atual;
		FileWriter fw = null;
		BufferedWriter buffWrite = null;
		while(true) {
			try {
				TimeUnit.SECONDS.sleep(timeUpdate);
				co2Atual = updateCO2();	//pega novo nivel de co2
				fw = new FileWriter(getArqCO2());
				buffWrite = new BufferedWriter(fw);
				buffWrite.append(co2Atual.toString() + '\n');	//escreve no arquivo de nivel de co2
				buffWrite.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				System.out.println("Problema na formatacao dos dados do nivel de CO2");
				return;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
	}
}
