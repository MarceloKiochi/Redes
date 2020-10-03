import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Cliente {
	public InetSocketAddress hostAddress = null;
	public SocketChannel client = null;
	
	public Cliente() throws IOException{		
		// Envia mensagem 1 ao Gerenciador, solicitando identificacao
		this.hostAddress = new InetSocketAddress("127.0.0.1", 9545);
		this.client = SocketChannel.open(hostAddress);
		this.client.configureBlocking(false);
		
		// Caso a conexao nao tenha sido finalizada
		if(client.isConnectionPending())
			client.finishConnect();
		
		String header = "1";
		String idCliente = "8";
		client.write(ByteBuffer.wrap((header + idCliente).getBytes()));
		
		// Espera pela mensagem 2, enviada pelo Gerenciador
		ByteBuffer msgServer;
		msgServer = ByteBuffer.allocate(256);		
		int bytesRead = 0;
		do {
			bytesRead = client.read(msgServer);
		} while(bytesRead <= 0);
		
		byte msgServerByte[] = msgServer.array();
		if(msgServerByte[0] == '2') {
			System.out.println("Cliente foi identificado pelo gerenciador!");
		}else {
			throw new RuntimeException("Problema no registro no gerenciador!");
		}
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

	// Converte um array de bytes para seu valor inteiro
	private static Integer byteToInt(int position, byte[] arr) {
		int num = 0;
		for(int i = 3; i >= 0; i--) {
			num = num<<8;
			num = num | (arr[i+position] & (int)0xff);
		}
		return num;
	}
	
	// Envia mensagem 6 (Configuracao dos parametros do gerenciador) ao gerenciador
	private void ConfiguraLimiares(String tipoParametro, int minVal, int maxVal) throws Exception {
		String header = "6";
		String msg = header + tipoParametro;
		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		byteArray.write(msg.getBytes());
		byteArray.write(intToByte(minVal));
		byteArray.write(intToByte(maxVal));
		ByteBuffer bufferWrite = ByteBuffer.allocate(256);
		
		try {
			bufferWrite = ByteBuffer.wrap(byteArray.toByteArray());
			client.write(bufferWrite);
			bufferWrite.clear();
		} catch(Exception e) {
			System.out.println("Servidor desligado, desligando conexao!");
			try {
				client.close();
			} catch (IOException e1) {;}			
			throw e;
		}		
	}

	// Envia mensagem 7 (Requisicao da ultima leitura do sensor) ao gerenciador
	// e recebe a mensagem 8 contendo a informacao requisitada
	private void RequisitaLeitura(String tipoParametro) throws Exception {
		String header = "7";
		String msg = header + tipoParametro;
		int bytesRead = 0;
		ByteBuffer bufferWrite = ByteBuffer.allocate(256);
		ByteBuffer bufferRead = ByteBuffer.allocate(256);
		
		try {
			bufferWrite = ByteBuffer.wrap(msg.getBytes());
			client.write(bufferWrite);
			bufferWrite.clear();
			
			do {
				bytesRead = client.read(bufferRead);
			} while(bytesRead <= 0);
			
			byte[] arr = bufferRead.array();	
			
			if(arr[0] == '8') {
				char tipo = (char)arr[1];
				int valor = byteToInt(2, arr);
				switch(tipo) {
				case '1':
					System.out.println("Temperatura: " + valor + "°C");
					break;
				case '2':
					System.out.println("Umidade do Solo: " + valor + "%");
					break;
				case '3':
					System.out.println("Nivel de CO2: " + valor + " ppmv");
					break;
				}
			} else {
				System.out.println("Erro na resposta do servidor!");
			}
			bufferRead.clear();
		} catch(Exception e) {
			System.out.println("Servidor desligado, desligando conexao!");
			try {
				client.close();
			} catch (IOException e1) {;}
			throw e;
		}
	}
	
	// Loop do Cliente, onde se recebe a entrada do usuario e envia as mensagens 6 e 7 ao gerenciador
	public void communicate() throws Exception {
		Scanner teclado = new Scanner(System.in);
		String tipoParametro;
		int minVal = 0, maxVal = 0;
		int comando = 0;

		while(true) {			
			// Menu do usuario
			System.out.println( 
				"   1 - Configurar limiares de temperatura\n" +
				"   2 - Configurar limiares de umidade\n" +
				"   3 - Configurar limiares de CO2\n" +
				"   4 - Checar temperatura\n" +
				"   5 - Checar nivel de umidade\n" +
				"   6 - Checar nivel de CO2\n" +
				"Insira o valor [1-6] correspondente ao comando: "
			);
			
			try {
				// Le comando e parametros
				comando = teclado.nextInt();
				tipoParametro = Integer.toString((((comando-1)%3)+1));
				if(comando >= 1 && comando <= 3) {
					System.out.print("Valor minimo: ");
					minVal = teclado.nextInt();			
					System.out.print("Valor maximo: ");
					maxVal = teclado.nextInt();
					
					// Trata valores minimo e maximo
					if(maxVal <= minVal) {
						System.out.println("Valores invalidos!");
						continue;
					}
					
					// Trata valores minimo e maximo para limiares de CO2 e umidade
					if(comando == 2){
						if(minVal < 0 || maxVal > 100) {
							System.out.println("Valores invalidos para os limiares!");					
							continue;
						}
					}					
					if(comando == 3){
						if(minVal < 0) {
							System.out.println("Valor invalido para o limiar inferior!");					
							continue;
						}
					}
					
					// Monta e envia a mensagem ao Gerenciador					
					ConfiguraLimiares(tipoParametro, minVal, maxVal);
					
				} else if(comando >= 4 && comando <= 6) {
					// Monta e envia a mensagem ao Gerenciador e recebe a leitura do sensor					
					RequisitaLeitura(tipoParametro);
				} else {
					System.out.println("Valor de comando invalido!");
					continue;
				}
			} catch(InputMismatchException e) {	// Caso o valor inserido nao seja um inteiro
				System.out.println("O valor deve ser um numero inteiro!");
				teclado.next();
				continue;
			}
		}
	}
	
	public static void main(String[] argc) throws UnknownHostException, IOException{
		try {
			Cliente cliente = new Cliente();
			cliente.communicate();
		}catch(Exception e) {
			System.out.println("Erro de conexao com gerenciador!");
		}
	}
}
