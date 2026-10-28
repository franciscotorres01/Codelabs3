package com.denkitronik.clienteservice2.cliente.services;

import com.denkitronik.clienteservice2.cliente.entities.Cliente;
import com.denkitronik.clienteservice2.cliente.entities.Region;
import com.denkitronik.clienteservice2.cliente.exceptions.ClienteNotFoundException;
import com.denkitronik.clienteservice2.cliente.exceptions.ClienteServiceException;
import com.denkitronik.clienteservice2.cliente.repositories.IClienteDao;
import com.denkitronik.clienteservice2.cliente.repositories.IRegionDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit tests — ClienteServiceImpl con Mockito")
class ClienteServiceImplTest {

    @Mock
    private IClienteDao clienteDao;      // mock: no toca la BD

    @Mock
    private IRegionDao regionDao;        // mock: no toca la BD

    @InjectMocks
    private ClienteServiceImpl clienteService;   // instancia real con los mocks inyectados

    private Cliente cliente;
    private Region region;

    @BeforeEach
    void setUp() {
        // Arrange compartido: datos de prueba reutilizados en todos los tests
        region = new Region();
        region.setId(1L);
        region.setNombre("Sudamérica");

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Linus");
        cliente.setApellido("Torvalds");
        cliente.setEmail("linus@kernel.org");
        cliente.setRegion(region);
    }

    // ---------- findAll ----------

    @Test
    @DisplayName("findAll — con clientes → devuelve la página")
    void findAll_conClientes_debeRetornarPagina() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 5);
        Page<Cliente> pagina = new PageImpl<>(List.of(cliente), pageable, 1);
        when(clienteDao.findAll(pageable)).thenReturn(pagina);

        // Act
        Page<Cliente> resultado = clienteService.findAll(pageable);

        // Assert
        // FALLO DELIBERADO (Paso 6): el valor correcto es 1
        assertThat(resultado.getContent()).hasSize(99);
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        verify(clienteDao, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("findAll — sin clientes → devuelve página vacía")
    void findAll_sinClientes_debeRetornarPaginaVacia() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 5);
        when(clienteDao.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        Page<Cliente> resultado = clienteService.findAll(pageable);

        // Assert
        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
    }

    // ---------- findById ----------

    @Test
    @DisplayName("findById — ID existente → devuelve el cliente")
    void findById_idExistente_debeRetornarCliente() {
        // Arrange
        when(clienteDao.findById(1L)).thenReturn(Optional.of(cliente));

        // Act
        Cliente resultado = clienteService.findById(1L);

        // Assert
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Linus");
        verify(clienteDao, times(1)).findById(1L);
    }

    @Test
    @DisplayName("findById — ID inexistente → lanza ClienteNotFoundException")
    void findById_idInexistente_debeLanzarExcepcion() {
        // Arrange
        when(clienteDao.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> clienteService.findById(999L))
                .isInstanceOf(ClienteNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ---------- save ----------

    @Test
    @DisplayName("save — cliente válido → devuelve el cliente guardado")
    void save_clienteValido_debeRetornarClienteGuardado() {
        // Arrange
        when(clienteDao.save(cliente)).thenReturn(cliente);

        // Act
        Cliente resultado = clienteService.save(cliente);

        // Assert
        assertThat(resultado).isSameAs(cliente);
        verify(clienteDao, times(1)).save(cliente);
    }

    @Test
    @DisplayName("save — email duplicado → lanza ClienteServiceException")
    void save_emailDuplicado_debeLanzarClienteServiceException() {
        // Arrange: simulamos que la BD rechaza por email duplicado
        when(clienteDao.save(cliente))
                .thenThrow(new DataIntegrityViolationException("duplicate key value"));

        // Act + Assert
        assertThatThrownBy(() -> clienteService.save(cliente))
                .isInstanceOf(ClienteServiceException.class)
                .hasMessageContaining("duplicados");
    }

    // ---------- delete ----------

    @Test
    @DisplayName("delete — ID existente → elimina el cliente")
    void delete_idExistente_debeEliminarCliente() {
        // Arrange
        when(clienteDao.existsById(1L)).thenReturn(true);

        // Act
        clienteService.delete(1L);

        // Assert
        verify(clienteDao, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("delete — ID inexistente → lanza excepción sin llamar deleteById")
    void delete_idInexistente_noDebeEliminar() {
        // Arrange
        when(clienteDao.existsById(999L)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> clienteService.delete(999L))
                .isInstanceOf(ClienteNotFoundException.class);

        // Verificación de comportamiento: deleteById NUNCA debe llamarse
        verify(clienteDao, never()).deleteById(any());
    }

    // ---------- update ----------

    @Test
    @DisplayName("update — ID existente → actualiza y retorna cliente modificado")
    void update_idExistente_debeActualizarYRetornar() {
        // Arrange
        Cliente datosNuevos = new Cliente();
        datosNuevos.setNombre("Linus Updated");
        datosNuevos.setApellido("Torvalds");
        datosNuevos.setEmail("linus2@kernel.org");
        datosNuevos.setRegion(region);

        when(clienteDao.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteDao.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Cliente resultado = clienteService.update(1L, datosNuevos);

        // Assert
        assertThat(resultado.getNombre()).isEqualTo("Linus Updated");
        assertThat(resultado.getEmail()).isEqualTo("linus2@kernel.org");
        verify(clienteDao, times(1)).findById(1L);
        verify(clienteDao, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("update — ID inexistente → lanza ClienteNotFoundException")
    void update_idInexistente_debeLanzarExcepcion() {
        // Arrange
        when(clienteDao.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> clienteService.update(999L, cliente))
                .isInstanceOf(ClienteNotFoundException.class)
                .hasMessageContaining("999");

        verify(clienteDao, never()).save(any(Cliente.class));
    }

    // ---------- findAllRegiones ----------

    @Test
    @DisplayName("findAllRegiones — devuelve la lista de regiones")
    void findAllRegiones_debeRetornarLista() {
        // Arrange
        when(regionDao.findAll()).thenReturn(List.of