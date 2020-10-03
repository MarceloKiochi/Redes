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

public class SensorTemperatura extends Thread{
	public InetSocketAddress hostAddress = null;	//endereco IP local host
	public SocketChannel client = null;	//socket
	private String idEquipamento = "1";	//id do sensor
	private String header;	//header da mensagem
	
	/* Inicializa a comunicacao do sensor de temperatura com 
	 * O gerenciador, depois disso ele aguarda ateh que a resposta do gerenciador 
	 * informe que ele se encontra Registrado
	 * Se houver problema no registro eh reportado erro*/
	public SensorTemperatura() throws IOException{
		byte msgServerByte[];
		ByteBuffer msgServer;
		this.hostAddress = new InetSocketAddress("127.0.0.1", 9545);	//cria um host de acesso
		this.client = SocketChannel.open(hostAddress);	//conexao com o IP e a porta
		this.client.configureBlocking(false);	//configura socket como nao bloqueante
		if(client.isConnectionPending())//Caso a conexao nao tenha sido finalizada
			client.finishConnect();
		
		header = "1";	//header da mensagem de identificacao
		client.write(ByteBuffer.wrap((header + idEquipamento).getBytes()));//Manda a mensagem de Identificacao: header + id
		msgServer = ByteBuffer.allocate(256);
		int bytesRead = 0;

		do {
			bytesRead = client.read(msgServer);	//salva mensagem em msgServer
		}while(bytesRead <= 0);/*Aguarda uma resposta do servidor*/
		msgServerByte = msgServer.array();	//passa mensagem para array de bytes
		if(msgServerByte[0] == '2') {	//mensagem de confirmacao de identificacao
			System.out.println("Equipamento registrado");
		}else {
			throw new RuntimeException("Problema no registro do equipamento no servidor!");
		}
	}
	
	// sensor faz a leitura da temperatura da estufa
	private byte[] readFileTemperatura() throws FileNotFoundException, IOException {
		FileReader fr = new FileReader(Temperatura.getArqTemperatura());	//arquivo que contem a temperatura
		BufferedReader buffRead = new BufferedReader(fr);	//leitor do arquivo
		int temperaturaInt = Integer.parseInt(buffRead.readLine());//Le como string, passa pra inteiro
		System.out.println("Leitura de temperatura:" + temperaturaInt + "°C");
		buffRead.close();
		
		//retorna o inteiro como vetor de bytes
		return intToByte(temperaturaInt);
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

	//envia as leituras de temperatura a cada segundo
	public void communicate() throws InterruptedException {
		ByteBuffer buffer = ByteBuffer.allocate(256);
		String msgSensor;
		header = "3";	//header da mensagem de envio de leitura
		
		
		while(true) {
			ByteArrayOutputStream byteArray = new ByteArrayOutputStream();	//estrutura para concatenar arrays de bytes
			TimeUnit.SECONDS.sleep(1);
			try {//monta a mensagem
				msgSensor = header + idEquipamento;//Header + id
				byteArray.write(msgSensor.getBytes());
				
				byteArray.write(readFileTemperatura());	//concatena com o novo valor da temperatura
			}catch(Exception e) {
				System.out.println("Problema ao abrir o arquivo para leitura!");
				return;
			}
			
			try {//envia a mensagem para o gerenciador					
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
			SensorTemperatura sensor = new SensorTemperatura();
			sensor.communicate();
		}catch(Exception e) {
			System.out.println("Erro de conexao com gerenciador!");
		}
	}
}
