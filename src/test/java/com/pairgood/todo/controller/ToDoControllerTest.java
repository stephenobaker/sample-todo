package com.pairgood.todo.controller;

import com.pairgood.todo.priority.Prioritizer;
import com.pairgood.todo.repository.ToDo;
import com.pairgood.todo.repository.ToDoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToDoControllerTest {

    @Mock
    private ToDoRepository repository;

    @Mock
    private Prioritizer<ToDo, Long> prioritizer;

    @InjectMocks
    private ToDoController controller;

    @Test
    void all_ShouldReturnAllOfTheToDos() {
        ToDo firstToDo = new ToDo("first", 1);
        ToDo secondToDo = new ToDo("second", 2);
        ToDo thirdToDo = new ToDo("thrid", 3);

        List<ToDo> toDos = Arrays.asList(firstToDo, secondToDo, thirdToDo);

        when(repository.findAllByOrderByPriorityAsc()).thenReturn(toDos);

        List<ToDo> all = controller.listAll();

        assertThat(all).isEqualTo(toDos);
    }

    @Test
    void all_ShouldReturnAllActiveToDos() {
        ToDo firstToDo = new ToDo("first", 1);
        ToDo thirdToDo = new ToDo("thrid", 2);

        List<ToDo> toDos = Arrays.asList(firstToDo, thirdToDo);

        when(repository.findByDoneOrderByPriorityAsc(false)).thenReturn(toDos);

        List<ToDo> all = controller.listActive();

        assertThat(all).isEqualTo(toDos);
    }

    @Test
    void newEmployee_ShouldSaveTheToDoItem() {
        ToDo newToDo = new ToDo();

        when(repository.count()).thenReturn(1L);

        controller.add(newToDo);

        assertThat(newToDo.getPriority()).isEqualTo(2);
        verify(repository).save(newToDo);
    }

    @Test
    void delete_ShouldDeleteToDoById() {

        controller.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void markAsDone_ShouldUpdateTheToDoItemToDone() {
        ToDo toDo = new ToDo();
        Optional<ToDo> optionalToDo = Optional.of(toDo);

        when(repository.findById(1L)).thenReturn(optionalToDo);

        controller.markAsDone(1L);

        assertThat(toDo.isDone()).isTrue();

        verify(repository).save(toDo);
    }

    @Test
    void markAsDone_ShouldNotUpdateAnyToDoItem_WhenNoToDoExistsForTheId() {
        Optional<ToDo> optionalToDo = Optional.empty();

        when(repository.findById(1L)).thenReturn(optionalToDo);

        controller.markAsDone(1L);

        verifyNoMoreInteractions(repository);
    }

    @Test
    void prioritizeUp_ShouldReprioritizeToDos() {
        List<ToDo> originalToDos = new ArrayList<>();
        List<ToDo> reprioritizedToDos = new ArrayList<>();

        when(repository.findAllByOrderByPriorityAsc()).thenReturn(originalToDos);
        when(prioritizer.prioritizeUp(originalToDos, 10L, 5L)).thenReturn(reprioritizedToDos);

        List<ToDo> actual = controller.prioritizeUp(10, 5);

        verify(repository).saveAll(reprioritizedToDos);

        assertThat(actual).isSameAs(reprioritizedToDos);
    }

    @Test
    void prioritizeDown_ShouldReprioritizeToDos() {
        List<ToDo> originalToDos = new ArrayList<>();
        List<ToDo> reprioritizedToDos = new ArrayList<>();

        when(repository.findAllByOrderByPriorityAsc()).thenReturn(originalToDos);
        when(prioritizer.prioritizeDown(originalToDos, 10L, 5L)).thenReturn(reprioritizedToDos);

        List<ToDo> actual = controller.prioritizeDown(10, 5);

        verify(repository).saveAll(reprioritizedToDos);

        assertThat(actual).isSameAs(reprioritizedToDos);
    }

    @Test
    void handleValidationExceptions_ShouldReturnAllErrorMessages() {
        BindingResult bindingResult = mock(BindingResult.class);

        when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(
                new FieldError("testOne", "testFieldOne", "testErrorMessageOne"),
                new FieldError("testTwo", "testFieldTwo", "testErrorMessageTwo"),
                new FieldError("testThree", "testFieldThree", "testErrorMessageThree")
        ));

        MethodArgumentNotValidException methodArgumentNotValidException =
                new MethodArgumentNotValidException(null, bindingResult);


        Map<String, String> errors = controller.handleValidationExceptions(methodArgumentNotValidException);

        assertThat(errors.size()).isEqualTo(3);

        assertThat(errors.containsKey("testFieldOne")).isTrue();
        assertThat(errors.containsKey("testFieldTwo")).isTrue();
        assertThat(errors.containsKey("testFieldThree")).isTrue();

        assertThat(errors.get("testFieldOne")).isEqualTo("testErrorMessageOne");
        assertThat(errors.get("testFieldTwo")).isEqualTo("testErrorMessageTwo");
        assertThat(errors.get("testFieldThree")).isEqualTo("testErrorMessageThree");

    }
}