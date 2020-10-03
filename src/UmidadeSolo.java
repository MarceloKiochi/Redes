import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

//classe utilizada para simular a umidade do solo
//umidade foi simulada a partir de arquivos de leitura e escrita

public class UmidadeSolo extends Thread{
	private static String pathUmidade = "umidade.txt";/*arquivo que simula umidade*/
	private static String pathContribuicaoUmidade = "contribuicaoUmidade.txt";	//arquivo de contribuicao do irrigador
	private static File arqUmidade = null;
	private static File arqContribuicaoUmidade = null;
	private static int contribuicaoUmidadeAmbiente = -1;	//ambiente fora da estufa sempre diminui a umidade
	private static int timeUpdadeSolo = 1;

	/* Metodo que retorna o arquivo de umidade para leitura ou escrita*/
	public static File getArqUmidade() throws IOException {
		if(arqUmidade == null)
			createFileUmidade();
		return arqUmidade;
	}
	
	/* Metodo que retorna o arquivo de contribuicao de umidade para leitura ou escrita*/
	public static File getArqContribuicaoUmidade() throws IOException {
		if(arqContribuicaoUmidade == null)
			createFileContribuicaoUmidade();
		return arqContribuicaoUmidade;
	}
	
	//metodo que muda o fator de contribuicao do irrigador (liga ou desliga)
	//contribuicao: eh o numero de unidades com que um atuador muda o valor do seu parametro
	//contribuicao do irrigador = 2. Significa que o irrigador aumenta a umidade de 2 em 2 (ligado)
	//contribuicao do irrigador = 0. Significa que o irrigador nao aumenta nem diminui a umidade (desligado)
	public static void setContribuicaoUmidadeEquip(Integer alteracao) {
		try {
			FileWriter fw = new FileWriter(getArqContribuicaoUmidade());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(alteracao.toString() + String.valueOf('\n'));
			buffWrite.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* Esse metodo realiza a criacao do arquivo de umidade(utilizado para simular a umidade) caso ele nao exista
	 * Caso ele ja se encontre criado eh feito uma checagem no arquivo, verificando se ele esta vazio(se isto ocorrer, passa o valor default)
	 * Se ja se encontrar dados no arquivo entao nao eh feito nada*/
	private static void createFileUmidade() throws IOException {
		Integer defaultUmidade = 30;
		arqUmidade = new File(pathUmidade);
		if(!arqUmidade.exists()) {
			arqUmidade.createNewFile();
			FileWriter fw = new FileWriter(getArqUmidade());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(defaultUmidade.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma umidade Inicial*/
			buffWrite.close();
		}else {
			FileReader fr = new FileReader(getArqUmidade());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio*/
				FileWriter fw = new FileWriter(arqUmidade);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(defaultUmidade.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma umidade Inicial*/
				buffWrite.close();
			}
			buffRead.close();
		}
	}
	
	/* Esse metodo realiza a criacao do arquivo de contribuicao(utilizado para simular a contribuicao do atuador na umidade) caso ele nao exista
	 * Caso ele ja se encontre criado eh feito uma checagem no arquivo, verificando se ele esta vazio(se isto ocorrer passa o valor default)
	 * Se ja se encontrar dados no arquivo entao nao eh feito nada*/
	private static void createFileContribuicaoUmidade() throws IOException{
		Integer contribuicaoDefault = 0;
		arqContribuicaoUmidade = new File(pathContribuicaoUmidade);
		if(!arqContribuicaoUmidade.exists()) {
			arqContribuicaoUmidade.createNewFile();
			FileWriter fw = new FileWriter(getArqContribuicaoUmidade());
			BufferedWriter buffWrite = new BufferedWriter(fw);
			buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));
			buffWrite.close();
		}else {//Se arquivo existir
			FileReader fr = new FileReader(getArqContribuicaoUmidade());
			BufferedReader buffRead = new BufferedReader(fr);
			if(!buffRead.ready()) {/*Se o arquivo se encontar criado mas estiver vazio(modificado)*/
				FileWriter fw = new FileWriter(arqContribuicaoUmidade);
				BufferedWriter buffWrite = new BufferedWriter(fw);
				buffWrite.append(contribuicaoDefault.toString() + String.valueOf('\n'));/*Inicializa o arquivo com uma contribuicao Inicial*/
				buffWrite.close();
			}
			buffRead.close();
		}
	}
	
	/* Esse metodo obtem a umidade atual lendo o arquivo umidade.txt
	 * E pega o fator de contribuicao do irrigador
	 * Tendo esses valores eh aplicado o fator de contribuicao do ambiente
	 * E retornado a umidade atualizada*/
	private int updateUmidade() throws FileNotFoundException, IOException{
		FileReader fr = new FileReader(getArqUmidade());
		BufferedReader buffRead = new BufferedReader(fr);
		Integer contribuicaoUmidadeEquip ;
		/*Lendo a umidade do arquivo*/
		Integer umidadeAtual = Integer.parseInt(buffRead.readLine());
		
		if(umidadeAtual <= 0) {//quando umidade chega a 0, para de diminuir
			contribuicaoUmidadeAmbiente = 0;
		} else {
			contribuicaoUmidadeAmbiente = -1;
		}
		
		/*Lendo contribuicao do irrigador no arquivo*/
		fr = new FileReader(getArqContribuicaoUmidade());
		buffRead = new BufferedReader(fr);
		contribuicaoUmidadeEquip = Integer.parseInt(buffRead.readLine());
		buffRead.close();

		return umidadeAtual + contribuicaoUmidadeAmbiente + contribuicaoUmidadeEquip;
	}
	
	//atualiza a umidade a cada segundo
	@Override
	public void run() {
		Integer umidadeSoloAtual;
		FileWriter fw = null;
		BufferedWriter buffWrite = null;
		while(true) {
			try {
				TimeUnit.SECONDS.sleep(timeUpdadeSolo);
				umidadeSoloAtual = updateUmidade();	//pega novo valor da umidade
				fw = new FileWriter(getArqUmidade());
				buffWrite = new BufferedWriter(fw);
				buffWrite.append(umidadeSoloAtual.toString() + '\n');	//escreve novo valor no arquivo da umidade
				buffWrite.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				System.out.println("Problema na formatacao dos dados da umidade");
				return;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
	}
}
