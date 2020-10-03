import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

public class SensorCO2 {
	public InetSocketAddress hostAddress = null;	//endereco IP local host
	public SocketChannel client = null;	//socket
	private String idEquipamento = "3";	//id do sensor
	private String header;	//header das mensagens
	
	/* Inicializa a comunicacao do sensor de co2 com 
	 * O gerenciador, depois disso ele aguarda ateh que a resposta do gerenciador 
	 * informe que ele se encontra Registrado
	 * Se houver problema no registro eh reportado erro*/
	public SensorCO2() throws IOException{
		byte msgServerByte[];
		ByteBuffer msgServer;
		this.hostAddress = new InetSocketAddress("127.0.0.1", 9545);	//cria um host de acesso
		this.client = SocketChannel.open(hostAddress);	//conexao com o IP e a porta
		this.client.configureBlocking(false);	//configura socket como nao-bloqueante
		if(client.isConnectionPending())//Caso a conexao nao tenha sido finalizada
			client.finishConnect();
		
		header = "1";	//header da mensagem de identificacao
		client.write(ByteBuffer.wrap((header + idEquipamento).getBytes()));//Manda a mensagem de Identificacao: header + id
		msgServer = ByteBuffer.allocate(256);
		//aguarda resposta do gerenciador
		int bytesRead = 0;
		do {
			bytesRead = client.read(msgServer);
		}while(bytesRead <= 0);/*Aguarda uma resposta do servidor*/
		msgServerByte = msgServer.array();
		if(msgServerByte[0] == '2') {	//se for confirmacao da identificacao
			System.out.println("Equipamento registrado");
		}else {
			throw new RuntimeException("Problema no registro do equipamento no servidor!");
		}
	}
	
	//retorna a leitura do nivel de CO2
	private byte[] readFileCO2() throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(CO2.getArqCO2());	//arquivo do CO2
		BufferedReader buffRead = new BufferedReader(fr);	//leitor
		int CO2Int = Integer.parseInt(buffRead.readLine());//Le como string, passa pra inteiro		
		System.out.println("Leitura de CO2:" + CO2Int + "ppm");
		buffRead.close();
		
		//retorna o valor do nivel de CO2 como um vetor de bytes
		return intToByte(CO2Int);
	}
	
	// Converte um inteiro para um vetor de bytes com seu valor binario
	private static byte[] intToByte(int inteiro) {
		int aux = inteiro;
		byte[] seqNumero = new byte[4];
		for(int i = 0; i < 4; i++) {
			seqNumero[i] = (byte) ((aux>>(i*8)) & (int)0xff);
		}
		
		return seqNumero;
	}

	// Envia a leitura do nivel de CO2 a cada segundo
	public void communicate() throws InterruptedException {
		ByteBuffer buffer = ByteBuffer.allocate(256);
		String msgSensor;
		header = "3";	//header da mensagem de envio de leitura		
		
		while(true) {
			TimeUnit.SECONDS.sleep(1);
			ByteArrayOutputStream byteArray = new ByteArrayOutputStream();	//estrutura para concatenar arrays de bytes
			try {//monta a mensagem
				msgSensor = header + idEquipamento;//Header + id
				byteArray.write(msgSensor.getBytes());
				
				byteArray.write(readFileCO2());	//concatena com o novo valor do nivel de CO2
			}catch(Exception e) {
				System.out.println("Problema ao abrir o arquivo para leitura!");
				return;
			}
			
			try {/*Envia a mensagem ao gerenciador*/
				buffer = ByteBuffer.wrap(byteArray.toByteArray());
				client.write(buffer);
				buffer.clear();
			}catch(Exception e) {
				System.out.println("Servidor desligado, desligando sensor!");
				try {
					client.close();
				} catch (IOException e1) {;}
				return;
			}
		}
	}
	
	public static void main(String[] argc) throws UnknownHostException, IOException{
		try {
			SensorCO2 sensor = new SensorCO2();
			sensor.communicate();
		}catch(Exception e) {
			System.out.println("Erro de conexao com gerenciador!");
		}
	}

}
