package br.ufs.dcomp.ChatRabbitMQ;

import java.util.Scanner;

import com.rabbitmq.client.*;

import java.io.IOException;

import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime; 

import java.io.*;
import com.google.protobuf.util.JsonFormat;
import java.util.*;


public class Chat {
  
  static String user;
  String input;
  
  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("ec2-54-197-1-117.compute-1.amazonaws.com"); // Alterar
    factory.setUsername("admin"); // Alterar
    factory.setPassword("password"); // Alterar
    factory.setVirtualHost("/");    
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    
    
    Consumer consumer = new DefaultConsumer(channel) {
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)           throws IOException {

        //Mapeando bytes para a mensagem protobuf 
        Msg.Mensagem msgfinal = Msg.Mensagem.parseFrom(body);
       
        //Resgatando metadados e a mensagem
        String emissor = msgfinal.getEmissor();
        String data = msgfinal.getData();
        String hora = msgfinal.getHora();
        String grupo = msgfinal.getGrupo();
        String message = msgfinal.getCorpo();
        
        // Se a msg vier do propio user, não faça nada
        if (emissor.equals(user)){
          System.out.print("");
        }
        
        else{
          // Se for msg normal
          if (grupo.equals("!")) {
            System.out.println("");
            System.out.println("(" + data + " às " + hora + ") " + emissor + " diz: " + message);
          }
          // msg de grupo
          else{
            System.out.println("");
            System.out.println("(" + data + " às " + hora + ") " + emissor + "#" + grupo + " diz: " + message);
          }
        }
          
        }
      
    };

    
    Scanner scan = new Scanner(System.in);

    // Pede o nome de usuario
    System.out.print("User: ");
    user = scan.nextLine();
    
    // Cria uma fila e um metodo consumidor para o usuario 
    channel.queueDeclare(user, false,   false,     false,       null);
    channel.basicConsume(user, true, consumer);
    
    
    String dest;
    String cmd;
    
    while(true) {
      
      int check = 0;
      // Loop infinito que le os inputs ate que um deles seja um input útil, se "check" permanecer sendo 0 após passar pelo laço abaixo, o input não foi útil
      System.out.print(">> ");
      String input = scan.nextLine();
      cmd = ">> ";
      
      while(true){
      
      // Comado de criar grupo
      if (input.startsWith("!addGroup")) {
        check = 1;
        String nomeGrupo = input.substring(10);
        channel.exchangeDeclare(nomeGrupo, "fanout", true);
        channel.queueBind(user, nomeGrupo, user);
        if(cmd.equals(">> ")){break;}
        System.out.print(cmd);
        input = scan.nextLine();
      }
      
      // Comando de adcionar users ao grupo
      if (input.startsWith("!addUser")) {
        check = 1;
        String[] substrings = input.split(" ");
        String novoUser = substrings[1];
        String grupo = substrings[2];
        channel.queueBind(novoUser, grupo, novoUser);
        if(cmd.equals(">> ")){break;}
        System.out.print(cmd);
        input = scan.nextLine();
        
      }
      
      // Comando de remover grupo
      if (input.startsWith("!removeGroup")) {
        check = 1;
        String[] substrings = input.split(" ");
        String grupo = substrings[1];
        channel.exchangeDelete(grupo);
        if(cmd.equals(">> ")){break;}
        System.out.print(cmd);
        input = scan.nextLine();
        
      }
      
      // Comando de remover membro de um grupo
      if (input.startsWith("!delFromGroup")) {
        check = 1;
        String[] substrings = input.split(" ");
        String delUser = substrings[1];
        String grupo = substrings[2];
        channel.queueUnbind(delUser, grupo, delUser);
        if(cmd.equals(">> ")){break;}
        System.out.print(cmd);
        input = scan.nextLine();
        
      }
      
      // Envio de msgs
      if (input.startsWith("@")) {
        
         check = 1;
         
         dest = input.substring(1);
        
         while (true){
          
           cmd = "@" + dest + ">> ";
           System.out.print("@" + dest + ">> ");
           input = scan.nextLine();
           
           // Se for outro comando, quebre o laço, e procure pelos outros formatos de input
           if (input.startsWith("#")){break;}
           if (input.startsWith("!")){break;}
           
           // Se for uma mensagem, a envie e aguarde o input da nova mensagem
           if (input.startsWith("@") == false) {
             
             
             // Coloca os metadados da mensagem e o corpo dela no proto.
             DateTimeFormatter data = DateTimeFormatter.ofPattern("dd/MM/yyyy");
             DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm");
             LocalDateTime now = LocalDateTime.now();
             
             Msg.Mensagem.Builder builderMetaDados = Msg.Mensagem.newBuilder();
             builderMetaDados.setData(data.format(now));
             builderMetaDados.setHora(hora.format(now));
             builderMetaDados.setEmissor(user);
             builderMetaDados.setGrupo("!");
             builderMetaDados.setCorpo(input);
            
             // Obtendo a mensagem final, com todos os dados necessários
             Msg.Mensagem msgfinal = builderMetaDados.build();
             byte[] buffer = msgfinal.toByteArray();
             
             
             channel.basicPublish("",     dest , null,  buffer);
             
          
           // Se nao for uma mensagem, quebre o laço, e procure pelos outros formatos de input
           } else {break;}
         }
      } 
      
      
      // Envio de msgs por grupo
      if (input.startsWith("#")) {
        
         check = 1;
         
         String grupo = input.substring(1);
        
         while (true){
         
           cmd = "#" + grupo + ">> ";
           System.out.print("#" + grupo + ">> ");
           input = scan.nextLine();
           
           // Se for outro comando, quebre o laço, e procure pelos outros formatos de input
           if (input.startsWith("@")){break;}
           if (input.startsWith("!")){break;}
           
           // Se for uma mensagem, a envie e aguarde o input da nova mensagem
           if (input.startsWith("#") == false) {
             
             
             // Coloca os metadados da mensagem e o corpo dela no proto.
             DateTimeFormatter data = DateTimeFormatter.ofPattern("dd/MM/yyyy");
             DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm");
             LocalDateTime now = LocalDateTime.now();
             
             Msg.Mensagem.Builder builderMetaDados = Msg.Mensagem.newBuilder();
             builderMetaDados.setData(data.format(now));
             builderMetaDados.setHora(hora.format(now));
             builderMetaDados.setEmissor(user);
             builderMetaDados.setGrupo(grupo);
             builderMetaDados.setCorpo(input);
            
             // Obtendo a mensagem final, com todos os dados necessários
             Msg.Mensagem msgfinal = builderMetaDados.build();
             byte[] buffer = msgfinal.toByteArray();
             
             
             channel.basicPublish(grupo,     "" , null,  buffer);
             
          
           // Se nao for uma mensagem, quebre o laço, e procure pelos outros formatos de input
           } else {break;}
         }
 
        
      } 
      
      // input inútil
      if (check == 0) {break;} 
      
      }
   
    }
    
    
  }
}
