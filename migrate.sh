#!/bin/bash
set -e
BLUE='\033[34m'
GREEN='\033[32m'
RESET='\033[0m'

move(){ src="$1"; dst="$2"; [ -f "$src" ] && { echo -e "${GREEN}mv $src -> $dst${RESET}"; mkdir -p "$(dirname "$dst")"; git mv "$src" "$dst"; }
}
update_pkg(){ file="$1"; old="$2"; new="$3"; [ -f "$file" ] && sed -i "s|^package $old|package $new|" "$file"; }
replace_import(){ old="$1"; new="$2"; grep -rl "$old" --include="*.kt" | xargs sed -i "s|$old|$new|g" || true; }

echo -e "${BLUE}Create folders${RESET}"
domains=(identity customer scheduling notification)
layers=(core usecase application infra)
modules=(core usecase application infra)
for mod in ${modules[@]}; do
  for dom in ${domains[@]}; do
    for layer in ${layers[@]}; do
      mkdir -p "$mod/src/main/kotlin/dev/vilquer/petcarescheduler/$dom/$layer"
    done
  done
done
mkdir -p core/src/main/kotlin/dev/vilquer/petcarescheduler/kernel

# kernel primitives
move core/src/main/kotlin/dev/vilquer/petcarescheduler/core/domain/valueobject/Email.kt core/src/main/kotlin/dev/vilquer/petcarescheduler/kernel/Email.kt
update_pkg core/src/main/kotlin/dev/vilquer/petcarescheduler/kernel/Email.kt dev.vilquer.petcarescheduler.core.domain.valueobject dev.vilquer.petcarescheduler.kernel
move core/src/main/kotlin/dev/vilquer/petcarescheduler/core/domain/valueobject/PhoneNumber.kt core/src/main/kotlin/dev/vilquer/petcarescheduler/kernel/PhoneNumber.kt
update_pkg core/src/main/kotlin/dev/vilquer/petcarescheduler/kernel/PhoneNumber.kt dev.vilquer.petcarescheduler.core.domain.valueobject dev.vilquer.petcarescheduler.kernel

# customer core
move core/src/main/kotlin/dev/vilquer/petcarescheduler/core/domain/entity/Tutor.kt core/src/main/kotlin/dev/vilquer/petcarescheduler/customer/core/domain/entity/Tutor.kt
update_pkg core/src/main/kotlin/dev/vilquer/petcarescheduler/customer/core/domain/entity/Tutor.kt dev.vilquer.petcarescheduler.core.domain.entity dev.vilquer.petcarescheduler.customer.core.domain.entity
move core/src/main/kotlin/dev/vilquer/petcarescheduler/core/domain/entity/Pet.kt core/src/main/kotlin/dev/vilquer/petcarescheduler/customer/core/domain/entity/Pet.kt
update_pkg core/src/main/kotlin/dev/vilquer/petcarescheduler/customer/core/domain/entity/Pet.kt dev.vilquer.petcarescheduler.core.domain.entity dev.vilquer.petcarescheduler.customer.core.domain.entity

# scheduling core
move core/src/main/kotlin/dev/vilquer/petcarescheduler/core/domain/entity/Event.kt core/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/core/domain/entity/Event.kt
update_pkg core/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/core/domain/entity/Event.kt dev.vilquer.petcarescheduler.core.domain.entity dev.vilquer.petcarescheduler.scheduling.core.domain.entity
move core/src/main/kotlin/dev/vilquer/petcarescheduler/core/domain/valueobject/Recurrence.kt core/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/core/domain/valueobject/Recurrence.kt
update_pkg core/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/core/domain/valueobject/Recurrence.kt dev.vilquer.petcarescheduler.core.domain.valueobject dev.vilquer.petcarescheduler.scheduling.core.domain.valueobject

# identity usecase
move usecase/src/main/kotlin/dev/vilquer/petcarescheduler/usecase/command/LoginCommand.kt usecase/src/main/kotlin/dev/vilquer/petcarescheduler/identity/usecase/command/LoginCommand.kt
update_pkg usecase/src/main/kotlin/dev/vilquer/petcarescheduler/identity/usecase/command/LoginCommand.kt dev.vilquer.petcarescheduler.usecase.command dev.vilquer.petcarescheduler.identity.usecase.command
move usecase/src/main/kotlin/dev/vilquer/petcarescheduler/usecase/contract/drivingports/AuthUseCase.kt usecase/src/main/kotlin/dev/vilquer/petcarescheduler/identity/usecase/contract/drivingports/AuthUseCase.kt
update_pkg usecase/src/main/kotlin/dev/vilquer/petcarescheduler/identity/usecase/contract/drivingports/AuthUseCase.kt dev.vilquer.petcarescheduler.usecase.contract.drivingports dev.vilquer.petcarescheduler.identity.usecase.contract.drivingports
move usecase/src/main/kotlin/dev/vilquer/petcarescheduler/usecase/contract/drivenports/PasswordHashPort.kt usecase/src/main/kotlin/dev/vilquer/petcarescheduler/identity/usecase/contract/drivenports/PasswordHashPort.kt
update_pkg usecase/src/main/kotlin/dev/vilquer/petcarescheduler/identity/usecase/contract/drivenports/PasswordHashPort.kt dev.vilquer.petcarescheduler.usecase.contract.drivenports dev.vilquer.petcarescheduler.identity.usecase.contract.drivenports

# identity application
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/adapter/input/security/BCryptHashAdapter.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/identity/application/adapter/input/security/BCryptHashAdapter.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/identity/application/adapter/input/security/BCryptHashAdapter.kt dev.vilquer.petcarescheduler.application.adapter.input.security dev.vilquer.petcarescheduler.identity.application.adapter.input.security
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/adapter/input/security/JwtProperties.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/identity/application/adapter/input/security/JwtProperties.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/identity/application/adapter/input/security/JwtProperties.kt dev.vilquer.petcarescheduler.application.adapter.input.security dev.vilquer.petcarescheduler.identity.application.adapter.input.security
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/adapter/input/rest/AuthController.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/identity/application/adapter/input/rest/AuthController.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/identity/application/adapter/input/rest/AuthController.kt dev.vilquer.petcarescheduler.application.adapter.input.rest dev.vilquer.petcarescheduler.identity.application.adapter.input.rest
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/service/AuthAppService.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/identity/application/service/AuthAppService.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/identity/application/service/AuthAppService.kt dev.vilquer.petcarescheduler.application.service dev.vilquer.petcarescheduler.identity.application.service
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/config/SecurityBeans.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/identity/application/config/SecurityBeans.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/identity/application/config/SecurityBeans.kt dev.vilquer.petcarescheduler.application.config dev.vilquer.petcarescheduler.identity.application.config
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/config/SecurityConfig.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/identity/application/config/SecurityConfig.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/identity/application/config/SecurityConfig.kt dev.vilquer.petcarescheduler.application.config dev.vilquer.petcarescheduler.identity.application.config

# customer commands
for f in CreatePetCommand DeletePetCommand UpdatePetCommand CreateTutorCommand DeleteTutorCommand UpdateTutorCommand; do
  move usecase/src/main/kotlin/dev/vilquer/petcarescheduler/usecase/command/$f.kt usecase/src/main/kotlin/dev/vilquer/petcarescheduler/customer/usecase/command/$f.kt
  update_pkg usecase/src/main/kotlin/dev/vilquer/petcarescheduler/customer/usecase/command/$f.kt dev.vilquer.petcarescheduler.usecase.command dev.vilquer.petcarescheduler.customer.usecase.command
done

# scheduling commands
for f in RegisterEventCommand UpdateEventCommand DeleteEventCommand ToggleEventCommand; do
  move usecase/src/main/kotlin/dev/vilquer/petcarescheduler/usecase/command/$f.kt usecase/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/usecase/command/$f.kt
  update_pkg usecase/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/usecase/command/$f.kt dev.vilquer.petcarescheduler.usecase.command dev.vilquer.petcarescheduler.scheduling.usecase.command
done

# customer driving usecases
customer_driving=(CreatePetUseCase DeletePetUseCase UpdatePetUseCase GetPetUseCase ListPetsUseCase CreateTutorUseCase DeleteTutorUseCase UpdateTutorUseCase GetTutorUseCase ListTutorsUseCase)
for f in ${customer_driving[@]}; do
  move usecase/src/main/kotlin/dev/vilquer/petcarescheduler/usecase/contract/drivingports/$f.kt usecase/src/main/kotlin/dev/vilquer/petcarescheduler/customer/usecase/contract/drivingports/$f.kt
  update_pkg usecase/src/main/kotlin/dev/vilquer/petcarescheduler/customer/usecase/contract/drivingports/$f.kt dev.vilquer.petcarescheduler.usecase.contract.drivingports dev.vilquer.petcarescheduler.customer.usecase.contract.drivingports
done

# scheduling driving usecases
scheduling_driving=(RegisterEventUseCase DeleteEventUseCase UpdateEventUseCase ToggleEventUseCase)
for f in ${scheduling_driving[@]}; do
  move usecase/src/main/kotlin/dev/vilquer/petcarescheduler/usecase/contract/drivingports/$f.kt usecase/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/usecase/contract/drivingports/$f.kt
  update_pkg usecase/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/usecase/contract/drivingports/$f.kt dev.vilquer.petcarescheduler.usecase.contract.drivingports dev.vilquer.petcarescheduler.scheduling.usecase.contract.drivingports
done

# driven ports
move usecase/src/main/kotlin/dev/vilquer/petcarescheduler/usecase/contract/drivenports/TutorRepositoryPort.kt usecase/src/main/kotlin/dev/vilquer/petcarescheduler/customer/usecase/contract/drivenports/TutorRepositoryPort.kt
update_pkg usecase/src/main/kotlin/dev/vilquer/petcarescheduler/customer/usecase/contract/drivenports/TutorRepositoryPort.kt dev.vilquer.petcarescheduler.usecase.contract.drivenports dev.vilquer.petcarescheduler.customer.usecase.contract.drivenports
move usecase/src/main/kotlin/dev/vilquer/petcarescheduler/usecase/contract/drivenports/PetRepositoryPort.kt usecase/src/main/kotlin/dev/vilquer/petcarescheduler/customer/usecase/contract/drivenports/PetRepositoryPort.kt
update_pkg usecase/src/main/kotlin/dev/vilquer/petcarescheduler/customer/usecase/contract/drivenports/PetRepositoryPort.kt dev.vilquer.petcarescheduler.usecase.contract.drivenports dev.vilquer.petcarescheduler.customer.usecase.contract.drivenports
move usecase/src/main/kotlin/dev/vilquer/petcarescheduler/usecase/contract/drivenports/EventRepositoryPort.kt usecase/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/usecase/contract/drivenports/EventRepositoryPort.kt
update_pkg usecase/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/usecase/contract/drivenports/EventRepositoryPort.kt dev.vilquer.petcarescheduler.usecase.contract.drivenports dev.vilquer.petcarescheduler.scheduling.usecase.contract.drivenports
move usecase/src/main/kotlin/dev/vilquer/petcarescheduler/usecase/contract/drivenports/ClockPort.kt usecase/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/usecase/contract/drivenports/ClockPort.kt
update_pkg usecase/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/usecase/contract/drivenports/ClockPort.kt dev.vilquer.petcarescheduler.usecase.contract.drivenports dev.vilquer.petcarescheduler.scheduling.usecase.contract.drivenports
move usecase/src/main/kotlin/dev/vilquer/petcarescheduler/usecase/contract/drivenports/NotificationPort.kt usecase/src/main/kotlin/dev/vilquer/petcarescheduler/notification/usecase/contract/drivenports/NotificationPort.kt
update_pkg usecase/src/main/kotlin/dev/vilquer/petcarescheduler/notification/usecase/contract/drivenports/NotificationPort.kt dev.vilquer.petcarescheduler.usecase.contract.drivenports dev.vilquer.petcarescheduler.notification.usecase.contract.drivenports

# results
customer_results=(PetSummary PetCreatedResult PetsPageResult PetDetailResult TutorCreatedResult TutorDetailResult TutorSummary TutorsPageResult)
for f in ${customer_results[@]}; do
  move usecase/src/main/kotlin/dev/vilquer/petcarescheduler/usecase/result/$f.kt usecase/src/main/kotlin/dev/vilquer/petcarescheduler/customer/usecase/result/$f.kt
  update_pkg usecase/src/main/kotlin/dev/vilquer/petcarescheduler/customer/usecase/result/$f.kt dev.vilquer.petcarescheduler.usecase.result dev.vilquer.petcarescheduler.customer.usecase.result
done
scheduling_results=(EventDetailResult EventRegisteredResult)
for f in ${scheduling_results[@]}; do
  move usecase/src/main/kotlin/dev/vilquer/petcarescheduler/usecase/result/$f.kt usecase/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/usecase/result/$f.kt
  update_pkg usecase/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/usecase/result/$f.kt dev.vilquer.petcarescheduler.usecase.result dev.vilquer.petcarescheduler.scheduling.usecase.result
done
# customer application
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/service/TutorAppService.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/customer/application/service/TutorAppService.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/customer/application/service/TutorAppService.kt dev.vilquer.petcarescheduler.application.service dev.vilquer.petcarescheduler.customer.application.service
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/service/PetAppService.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/customer/application/service/PetAppService.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/customer/application/service/PetAppService.kt dev.vilquer.petcarescheduler.application.service dev.vilquer.petcarescheduler.customer.application.service
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/mapper/PetDtoMapper.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/customer/application/mapper/PetDtoMapper.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/customer/application/mapper/PetDtoMapper.kt dev.vilquer.petcarescheduler.application.mapper dev.vilquer.petcarescheduler.customer.application.mapper
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/mapper/TutorDtoMapper.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/customer/application/mapper/TutorDtoMapper.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/customer/application/mapper/TutorDtoMapper.kt dev.vilquer.petcarescheduler.application.mapper dev.vilquer.petcarescheduler.customer.application.mapper
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/adapter/input/rest/PetController.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/customer/application/adapter/input/rest/PetController.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/customer/application/adapter/input/rest/PetController.kt dev.vilquer.petcarescheduler.application.adapter.input.rest dev.vilquer.petcarescheduler.customer.application.adapter.input.rest
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/adapter/input/rest/TutorController.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/customer/application/adapter/input/rest/TutorController.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/customer/application/adapter/input/rest/TutorController.kt dev.vilquer.petcarescheduler.application.adapter.input.rest dev.vilquer.petcarescheduler.customer.application.adapter.input.rest
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/adapter/input/rest/PublicController.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/customer/application/adapter/input/rest/PublicController.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/customer/application/adapter/input/rest/PublicController.kt dev.vilquer.petcarescheduler.application.adapter.input.rest dev.vilquer.petcarescheduler.customer.application.adapter.input.rest

# scheduling application
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/service/EventAppService.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/application/service/EventAppService.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/application/service/EventAppService.kt dev.vilquer.petcarescheduler.application.service dev.vilquer.petcarescheduler.scheduling.application.service
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/mapper/EventDtoMapper.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/application/mapper/EventDtoMapper.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/application/mapper/EventDtoMapper.kt dev.vilquer.petcarescheduler.application.mapper dev.vilquer.petcarescheduler.scheduling.application.mapper
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/adapter/input/rest/EventController.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/application/adapter/input/rest/EventController.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/application/adapter/input/rest/EventController.kt dev.vilquer.petcarescheduler.application.adapter.input.rest dev.vilquer.petcarescheduler.scheduling.application.adapter.input.rest
move application/src/main/kotlin/dev/vilquer/petcarescheduler/application/adapter/input/scheduler/EventReminderScheduler.kt application/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/application/adapter/input/scheduler/EventReminderScheduler.kt
update_pkg application/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/application/adapter/input/scheduler/EventReminderScheduler.kt dev.vilquer.petcarescheduler.application.adapter.input.scheduler dev.vilquer.petcarescheduler.scheduling.application.adapter.input.scheduler
# scheduling infra
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/external/EventRepositoryAdapter.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/infra/adapter/output/external/EventRepositoryAdapter.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/infra/adapter/output/external/EventRepositoryAdapter.kt dev.vilquer.petcarescheduler.infra.adapter.output.external dev.vilquer.petcarescheduler.scheduling.infra.adapter.output.external
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/external/ClockAdapter.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/infra/adapter/output/external/ClockAdapter.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/infra/adapter/output/external/ClockAdapter.kt dev.vilquer.petcarescheduler.infra.adapter.output.external dev.vilquer.petcarescheduler.scheduling.infra.adapter.output.external
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/persistence/jpa/entity/EventJpa.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/infra/adapter/output/persistence/jpa/entity/EventJpa.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/infra/adapter/output/persistence/jpa/entity/EventJpa.kt dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity dev.vilquer.petcarescheduler.scheduling.infra.adapter.output.persistence.jpa.entity
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/persistence/jpa/embeddable/RecurrenceEmb.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/infra/adapter/output/persistence/jpa/embeddable/RecurrenceEmb.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/infra/adapter/output/persistence/jpa/embeddable/RecurrenceEmb.kt dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.embeddable dev.vilquer.petcarescheduler.scheduling.infra.adapter.output.persistence.jpa.embeddable
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/persistence/jpa/mappers/EventMapper.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/infra/adapter/output/persistence/jpa/mappers/EventMapper.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/infra/adapter/output/persistence/jpa/mappers/EventMapper.kt dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers dev.vilquer.petcarescheduler.scheduling.infra.adapter.output.persistence.jpa.mappers
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/persistence/jpa/mappers/RecurrenceMapper.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/infra/adapter/output/persistence/jpa/mappers/RecurrenceMapper.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/infra/adapter/output/persistence/jpa/mappers/RecurrenceMapper.kt dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers dev.vilquer.petcarescheduler.scheduling.infra.adapter.output.persistence.jpa.mappers
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/persistence/jpa/repository/EventJpaRepository.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/infra/adapter/output/persistence/jpa/repository/EventJpaRepository.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/scheduling/infra/adapter/output/persistence/jpa/repository/EventJpaRepository.kt dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository dev.vilquer.petcarescheduler.scheduling.infra.adapter.output.persistence.jpa.repository

# customer infra
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/external/TutorRepositoryAdapter.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/external/TutorRepositoryAdapter.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/external/TutorRepositoryAdapter.kt dev.vilquer.petcarescheduler.infra.adapter.output.external dev.vilquer.petcarescheduler.customer.infra.adapter.output.external
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/external/PetRepositoryAdapter.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/external/PetRepositoryAdapter.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/external/PetRepositoryAdapter.kt dev.vilquer.petcarescheduler.infra.adapter.output.external dev.vilquer.petcarescheduler.customer.infra.adapter.output.external
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/persistence/jpa/entity/TutorJpa.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/persistence/jpa/entity/TutorJpa.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/persistence/jpa/entity/TutorJpa.kt dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity dev.vilquer.petcarescheduler.customer.infra.adapter.output.persistence.jpa.entity
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/persistence/jpa/entity/PetJpa.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/persistence/jpa/entity/PetJpa.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/persistence/jpa/entity/PetJpa.kt dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.entity dev.vilquer.petcarescheduler.customer.infra.adapter.output.persistence.jpa.entity
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/persistence/jpa/mappers/TutorMapper.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/persistence/jpa/mappers/TutorMapper.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/persistence/jpa/mappers/TutorMapper.kt dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers dev.vilquer.petcarescheduler.customer.infra.adapter.output.persistence.jpa.mappers
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/persistence/jpa/mappers/PetMapper.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/persistence/jpa/mappers/PetMapper.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/persistence/jpa/mappers/PetMapper.kt dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.mappers dev.vilquer.petcarescheduler.customer.infra.adapter.output.persistence.jpa.mappers
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/persistence/jpa/repository/TutorJpaRepository.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/persistence/jpa/repository/TutorJpaRepository.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/persistence/jpa/repository/TutorJpaRepository.kt dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository dev.vilquer.petcarescheduler.customer.infra.adapter.output.persistence.jpa.repository
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/persistence/jpa/repository/PetJpaRepository.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/persistence/jpa/repository/PetJpaRepository.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/customer/infra/adapter/output/persistence/jpa/repository/PetJpaRepository.kt dev.vilquer.petcarescheduler.infra.adapter.output.persistence.jpa.repository dev.vilquer.petcarescheduler.customer.infra.adapter.output.persistence.jpa.repository

# notification infra
move infra/src/main/kotlin/dev/vilquer/petcarescheduler/infra/adapter/output/notification/NotificationAdapter.kt infra/src/main/kotlin/dev/vilquer/petcarescheduler/notification/infra/adapter/output/notification/NotificationAdapter.kt
update_pkg infra/src/main/kotlin/dev/vilquer/petcarescheduler/notification/infra/adapter/output/notification/NotificationAdapter.kt dev.vilquer.petcarescheduler.infra.adapter.output.notification dev.vilquer.petcarescheduler.notification.infra.adapter.output.notification

# import replacements
replace_import dev.vilquer.petcarescheduler.core.domain.valueobject.Email dev.vilquer.petcarescheduler.kernel.Email
replace_import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber dev.vilquer.petcarescheduler.kernel.PhoneNumber
replace_import dev.vilquer.petcarescheduler.core.domain.entity.Tutor dev.vilquer.petcarescheduler.customer.core.domain.entity.Tutor
replace_import dev.vilquer.petcarescheduler.core.domain.entity.TutorId dev.vilquer.petcarescheduler.customer.core.domain.entity.TutorId
replace_import dev.vilquer.petcarescheduler.core.domain.entity.Pet dev.vilquer.petcarescheduler.customer.core.domain.entity.Pet
replace_import dev.vilquer.petcarescheduler.core.domain.entity.PetId dev.vilquer.petcarescheduler.customer.core.domain.entity.PetId
replace_import dev.vilquer.petcarescheduler.core.domain.entity.Event dev.vilquer.petcarescheduler.scheduling.core.domain.entity.Event
replace_import dev.vilquer.petcarescheduler.core.domain.entity.EventId dev.vilquer.petcarescheduler.scheduling.core.domain.entity.EventId
replace_import dev.vilquer.petcarescheduler.core.domain.entity.EventType dev.vilquer.petcarescheduler.scheduling.core.domain.entity.EventType
replace_import dev.vilquer.petcarescheduler.core.domain.entity.Status dev.vilquer.petcarescheduler.scheduling.core.domain.entity.Status
replace_import dev.vilquer.petcarescheduler.core.domain.valueobject.Recurrence dev.vilquer.petcarescheduler.scheduling.core.domain.valueobject.Recurrence
replace_import dev.vilquer.petcarescheduler.core.domain.valueobject.Frequency dev.vilquer.petcarescheduler.scheduling.core.domain.valueobject.Frequency
replace_import dev.vilquer.petcarescheduler.usecase.result dev.vilquer.petcarescheduler.customer.usecase.result
replace_import dev.vilquer.petcarescheduler.usecase.command dev.vilquer.petcarescheduler.customer.usecase.command
replace_import dev.vilquer.petcarescheduler.customer.usecase.command.LoginCommand dev.vilquer.petcarescheduler.identity.usecase.command.LoginCommand
replace_import dev.vilquer.petcarescheduler.usecase.contract.drivingports dev.vilquer.petcarescheduler.customer.usecase.contract.drivingports
replace_import dev.vilquer.petcarescheduler.customer.usecase.contract.drivingports.AuthUseCase dev.vilquer.petcarescheduler.identity.usecase.contract.drivingports.AuthUseCase
replace_import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PasswordHashPort dev.vilquer.petcarescheduler.identity.usecase.contract.drivenports.PasswordHashPort
replace_import dev.vilquer.petcarescheduler.usecase.contract.drivenports.EventRepositoryPort dev.vilquer.petcarescheduler.scheduling.usecase.contract.drivenports.EventRepositoryPort
replace_import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ClockPort dev.vilquer.petcarescheduler.scheduling.usecase.contract.drivenports.ClockPort
replace_import dev.vilquer.petcarescheduler.usecase.contract.drivenports.NotificationPort dev.vilquer.petcarescheduler.notification.usecase.contract.drivenports.NotificationPort
replace_import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PetRepositoryPort dev.vilquer.petcarescheduler.customer.usecase.contract.drivenports.PetRepositoryPort
replace_import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort dev.vilquer.petcarescheduler.customer.usecase.contract.drivenports.TutorRepositoryPort

# run tests
./gradlew test
