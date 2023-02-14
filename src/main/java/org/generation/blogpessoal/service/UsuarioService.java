package org.generation.blogpessoal.service;

import java.nio.charset.Charset;
import java.util.Optional;

import org.apache.commons.codec.binary.Base64;
import org.generation.blogpessoal.model.Usuario;
import org.generation.blogpessoal.model.UsuarioLogin;
import org.generation.blogpessoal.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

	@Autowired
	private UsuarioRepository usuarioRepository;

	// função que criptografa a senha digitada pelo usuario

	private String criptografarSenha(String senha) {

		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

		return encoder.encode(senha);

	}

	// função que cadastra um novo usuario no banco de dados, desde que esse email
	// já não esteja sendo utilizado por outra pessoa

	public Optional<Usuario> cadastrarUsuario(Usuario usuario) {

		// verifica dentro do banco se ja existe um usuario com o email a ser cadastrado
		if (usuarioRepository.findByUsuario(usuario.getUsuario()).isPresent())
			return Optional.empty();

		// se não tiver o mesmo email, a função criptografa a senha nova do usuario
		// antes de mandar o objeto para o banco
		usuario.setSenha(criptografarSenha(usuario.getSenha()));

		// por ultimo com a senha já criptografada o usuario é salvo
		return Optional.of(usuarioRepository.save(usuario));

	}

// algoritmo que gera o token para o usuário logado 
	private String gerarBasicToken(String usuario, String senha) {

		String token = usuario + ":" + senha;
		byte[] tokenBase64 = Base64.encodeBase64(token.getBytes(Charset.forName("US-ASCII")));
		return "Basic " + new String(tokenBase64);
	}

	// função que compara as senhas e verifica se a senha cadastrada no banco de
	// dados é a mesma senha inserida pelo usuário no login
	private boolean compararSenhas(String senhaDigitada, String senhaBanco) {

		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

		return encoder.matches(senhaDigitada, senhaBanco);

	}

	// função de logar um usuario no blog pessoal - Para autenticar um usuario,
	// primeiro eu verifico se esse usuario existe no banco de dados com o email que
	// ele está logando, comparto se é o mesmo email, e comparo também a senha, pra
	// ver se a senha cadastrada é a mesma que o usuario está inserindo
	public Optional<UsuarioLogin> autenticarUsuario(Optional<UsuarioLogin> usuarioLogin) {

		Optional<Usuario> usuario = usuarioRepository.findByUsuario(usuarioLogin.get().getUsuario());

		// se o usuario existir, eu checo se a senha digitada é a mesma salva
		if (usuario.isPresent()) {
			// se a senha e o email estiverem corretos, eu monto o objeto do usuario logado
			// já com o token, permitindo que ele faça todas as requisições da nossa api
			if (compararSenhas(usuarioLogin.get().getSenha(), usuario.get().getSenha())) {

				usuarioLogin.get().setId(usuario.get().getId());
				usuarioLogin.get().setNome(usuario.get().getNome());
				usuarioLogin.get().setFoto(usuario.get().getFoto());
				usuarioLogin.get()
						.setToken(gerarBasicToken(usuarioLogin.get().getUsuario(), usuarioLogin.get().getSenha()));
				usuarioLogin.get().setSenha(usuario.get().getSenha());

				return usuarioLogin;

			}
		}

		return Optional.empty();
	}

	public Optional<Usuario> atualizarUsuario(Usuario usuario) {

		if (usuarioRepository.findById(usuario.getId()).isPresent()) {

			Optional<Usuario> buscaUsuario = usuarioRepository.findByUsuario(usuario.getUsuario());

			if ((buscaUsuario.isPresent()) && (buscaUsuario.get().getId() != usuario.getId()))
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já existe!", null);

			usuario.setSenha(criptografarSenha(usuario.getSenha()));

			return Optional.ofNullable(usuarioRepository.save(usuario));

		}

		return Optional.empty();

	}
}
