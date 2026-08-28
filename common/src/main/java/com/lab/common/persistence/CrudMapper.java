package com.lab.common.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

/** Compatibility facade that keeps service code concise while using MyBatis-Plus mappers. */
public interface CrudMapper<T> extends BaseMapper<T> {
    default T save(T entity) {
        int affected = entityId(entity) == null ? insert(entity) : updateById(entity);
        if (affected != 1) {
            throw new IllegalStateException("Persistence update was rejected, possibly because the record changed concurrently");
        }
        return entity;
    }

    default T saveAndFlush(T entity) {
        return save(entity);
    }

    default List<T> saveAll(Iterable<T> entities) {
        java.util.ArrayList<T> result = new java.util.ArrayList<>();
        entities.forEach(entity -> result.add(save(entity)));
        return result;
    }

    default Optional<T> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default List<T> findAll() {
        return selectList(null);
    }

    default boolean existsById(Long id) {
        return selectById(id) != null;
    }

    default void delete(T entity) {
        Object id = entityId(entity);
        if (id != null) deleteById((java.io.Serializable) id);
    }

    private Object entityId(T entity) {
        if (entity == null) return null;
        TableInfo table = TableInfoHelper.getTableInfo(entity.getClass());
        if (table == null || table.getKeyProperty() == null) {
            throw new IllegalStateException("MyBatis table metadata is missing for " + entity.getClass().getName());
        }
        try {
            Field field = entity.getClass().getField(table.getKeyProperty());
            return field.get(entity);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read entity key " + table.getKeyProperty(), exception);
        }
    }
}
